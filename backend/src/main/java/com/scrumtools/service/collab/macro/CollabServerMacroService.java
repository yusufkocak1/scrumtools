package com.scrumtools.service.collab.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumtools.entity.CollabMacro;
import com.scrumtools.entity.CollabMacroRun;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.MacroRunStatus;
import com.scrumtools.entity.enums.MacroTriggerType;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.repository.CollabMacroRepository;
import com.scrumtools.repository.CollabMacroRunRepository;
import com.scrumtools.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Sunucu tarafı makro yürütmesinin düzenleyicisi (plan Faz 5).
 *
 * <p><b>Makro kimin yetkisiyle koşar?</b> §9.2 "çalıştıranın yetkisiyle" diyor
 * ama webhook geldiğinde çalıştıran bir insan yok. Cevap: <b>onaylayan kişinin</b>
 * yetkisiyle. Onay zaten "bu kodu okudum ve arkasındayım" beyanıdır ve
 * {@code source_hash}'e bağlıdır — kaynak değişirse onay düşer, webhook da
 * kendiliğinden çalışmayı bırakır. Yazarın yetkisini kullanmak yanlış olurdu:
 * onaysız makroyu yalnızca yazarının çalıştırabilmesi bir <i>tolerans</i>,
 * güvenilirlik beyanı değil.
 *
 * <p><b>İşlem sınırları:</b> yürütme kuyruk iş parçacığında ve açık bir DB
 * işlemi <b>dışında</b> yapılır. 10 saniyelik bir betik boyunca Hikari
 * bağlantısını tutmak, dar sunucuda (D3) havuzu birkaç makroyla tüketirdi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollabServerMacroService {

    /** Betiğin kendi yürütme süresi (plan Faz 5: 10 sn). */
    private static final long EXEC_TIMEOUT_MS = 10_000;

    /**
     * Kuyrukta bekleme dâhil üst sınır. Yürütme sınırından belirgin şekilde
     * uzun: sırada bekleyen bir makro, hiç başlamadan "zaman aşımı" damgası
     * yememeli.
     */
    private static final long TOTAL_TIMEOUT_MS = 45_000;

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_LOG_CHARS = 20_000;

    private final CollabMacroRepository macroRepository;
    private final CollabMacroRunRepository runRepository;
    private final CollabMacroService macroService;
    private final EntitlementService entitlementService;
    private final CollabMacroQueue queue;
    private final ServerMacroRunner runner;
    private final ObjectMapper objectMapper;

    /**
     * Webhook ile tetiklenen çalıştırma.
     *
     * @param rawBody  imza <b>ham gövde</b> üzerinden doğrulanır; ayrıştırılmış
     *                 ve yeniden serileştirilmiş JSON, anahtar sırası değiştiği
     *                 için farklı bir imza üretirdi
     * @param signature {@code X-ScrumTools-Signature} başlığı (hex HMAC-SHA256)
     */
    public MacroRunStatus triggerByWebhook(UUID macroId, String signature, String rawBody) {
        Prepared prepared = prepare(macroId, signature, rawBody);

        ServerMacroRunner.Result result = queue.submit(
                "webhook:" + macroId, TOTAL_TIMEOUT_MS,
                () -> runner.run(prepared.source(), prepared.projectId(),
                        prepared.identityEmail(), prepared.payload(), EXEC_TIMEOUT_MS));

        finish(prepared.runId(), result);
        return result.status();
    }

    private record Prepared(UUID runId, UUID projectId, String source,
                            String identityEmail, Map<String, Object> payload) {
    }

    /**
     * Yetki kararı ve {@code RUNNING} kaydı — yürütme başlamadan önce.
     *
     * <p><b>Neden {@code @Transactional} yok:</b> bu metot aynı sınıfın içinden
     * çağrılıyor, yani Spring proxy'si devrede değil — anotasyon konsa da hiçbir
     * işlem açılmazdı ve okuyan birini yanıltırdı. İhtiyaç da yok:
     * {@code findByIdForExecution} ilişkileri fetch join ile getiriyor (tembel
     * erişim kalmıyor) ve repository çağrılarının her biri kendi işlemini açıyor.
     */
    private Prepared prepare(UUID macroId, String signature, String rawBody) {
        CollabMacro macro = macroRepository.findByIdForExecution(macroId)
                .orElseThrow(() -> new IllegalArgumentException("Makro bulunamadı."));

        if (macro.getTriggerType() != MacroTriggerType.ON_WEBHOOK) {
            throw new SecurityException("Bu makro webhook ile tetiklenecek şekilde tanımlanmamış.");
        }
        verifySignature(macro, signature, rawBody);

        entitlementService.assertFeature(macro.getProject().getOrganization(), PlanFeature.COLLAB_MACRO);

        User identity = macro.getApprovedBy();
        if (identity == null) {
            // isApproved() zaten aşağıdaki denialReason'da kontrol ediliyor; bu
            // ayrı kontrol, "onaylı ama onaylayanı silinmiş" durumunu yakalar.
            throw new SecurityException("Makro onaylı değil; webhook tetikleyicisi çalışmaz.");
        }

        String denial = macroService.denialReason(
                macro, identity, macro.getProject().getId(), MacroTriggerType.ON_WEBHOOK);
        if (denial != null) {
            recordRun(macro, identity, MacroRunStatus.DENIED, null, denial, 0L);
            throw new SecurityException(denial);
        }

        CollabMacroRun run = recordRun(macro, identity, MacroRunStatus.RUNNING, null, null, null);
        return new Prepared(run.getId(), macro.getProject().getId(), macro.getSource(),
                identity.getEmail(), parsePayload(rawBody));
    }

    /** Sonucu çalıştırma günlüğüne yazar (işlem gerekçesi için bkz. {@link #prepare}). */
    private void finish(UUID runId, ServerMacroRunner.Result result) {
        runRepository.findById(runId).ifPresent(run -> {
            run.setStatus(result.status());
            run.setFinishedAt(LocalDateTime.now());
            run.setDurationMs(result.durationMs());
            run.setLog(truncate(result.log()));
            run.setError(truncate(result.error()));
            // Sunucu makrosu ortak dokümana dokunamaz (ServerMacroApi): bu alan
            // sunucu tarafında her zaman false.
            run.setWroteDocument(false);
            runRepository.save(run);
        });
        if (result.status() != MacroRunStatus.SUCCESS) {
            log.warn("Sunucu makrosu başarısız: run={} durum={} hata={}",
                    runId, result.status(), result.error());
        }
    }

    // ─── İmza ────────────────────────────────────────────────────────────────

    /**
     * Yeni bir webhook sırrı üretir ve döndürür.
     *
     * <p>Sır yalnızca burada, üretildiği anda görünür — veritabanında saklanan
     * değer aynı olsa da arayüz onu bir daha göstermez. Bunu yapmanın sebebi
     * kullanıcıyı "kaydettin mi?" diye zorlamak: sır ekranda kalıcı olarak
     * durursa, ekran görüntüsü ve destek talebi yoluyla dolaşmaya başlar.
     */
    @Transactional
    public String rotateWebhookSecret(UUID macroId) {
        CollabMacro macro = macroRepository.findById(macroId)
                .orElseThrow(() -> new IllegalArgumentException("Makro bulunamadı."));
        macroService.assertCanManage(macro.getProject().getId());

        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        String secret = HexFormat.of().formatHex(bytes);
        macro.setWebhookSecret(secret);
        macro.setUpdatedAt(LocalDateTime.now());
        macroRepository.save(macro);
        return secret;
    }

    private void verifySignature(CollabMacro macro, String signature, String rawBody) {
        String secret = macro.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new SecurityException("Bu makro için webhook sırrı tanımlanmamış.");
        }
        if (signature == null || signature.isBlank()) {
            throw new SecurityException("X-ScrumTools-Signature başlığı eksik.");
        }

        String expected = hmac(secret, rawBody == null ? "" : rawBody);
        // Sabit süreli karşılaştırma: normal String.equals ilk farklı bayta kadar
        // harcadığı süreyle imzayı bayt bayt tahmin etmeye kapı aralar.
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Webhook imzası doğrulanamadı.");
        }
    }

    private String hmac(String secret, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("İmza hesaplanamadı", e);
        }
    }

    // ─── Yardımcılar ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(rawBody, Map.class);
        } catch (Exception e) {
            // Gövde JSON değilse makro yine çalışsın: tetikleyicinin varlığı da
            // tek başına anlamlı bir sinyal olabilir.
            return Map.of("raw", rawBody);
        }
    }

    private CollabMacroRun recordRun(CollabMacro macro, User identity, MacroRunStatus status,
                                     String log, String error, Long durationMs) {
        return runRepository.save(CollabMacroRun.builder()
                .macro(macro)
                .triggeredBy(identity)
                .triggerType(MacroTriggerType.ON_WEBHOOK)
                .status(status)
                .startedAt(LocalDateTime.now())
                .finishedAt(status == MacroRunStatus.RUNNING ? null : LocalDateTime.now())
                .durationMs(durationMs)
                .log(truncate(log))
                .error(truncate(error))
                .wroteDocument(false)
                .build());
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_LOG_CHARS
                ? value : value.substring(0, MAX_LOG_CHARS) + "\n… (kırpıldı)";
    }
}

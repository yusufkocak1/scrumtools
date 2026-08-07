package com.scrumtools.service.collab.macro;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.*;
import com.scrumtools.repository.*;
import com.scrumtools.service.ActivityService;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.collab.CollabPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Makro yaşam döngüsü ve <b>çalıştırma izni</b> (COLLAB_WORKSPACE_PLAN.md §9).
 *
 * <p>Yürütmenin kendisi burada değil tarayıcıda olur (K8). Sunucunun rolü üç
 * şeyle sınırlı: kaynağı saklamak, <b>çalıştırılabilir mi</b> sorusuna cevap
 * vermek ve sonucu denetime yazmak. Bu ayrım kasıtlı — betiği sunucuda koşturmak
 * dar sunucuda (D3) tek bir sonsuz döngüyle tüm CPU'yu yiyecek bir kapı açardı.
 *
 * <p>İzin kararının tek yeri {@link #beginRun}: istemci "onaylı mı" sorusunu
 * kendi kendine cevaplayamaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollabMacroService {

    /** Günlük ve hata metinlerinin üst sınırı; makro sonsuz log basabilir. */
    private static final int MAX_LOG_CHARS = 20_000;

    private final CollabMacroRepository macroRepository;
    private final CollabMacroRunRepository runRepository;
    private final CollabDocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CollabPermissionService permissionService;
    private final MacroApiScanner scanner;
    private final EntitlementService entitlementService;
    private final ActivityService activityService;
    private final SimpMessagingTemplate messagingTemplate;

    /** {@link #beginRun} sonucu: istemcinin çalıştırmak için ihtiyaç duyduğu her şey. */
    public record RunTicket(UUID runId, String source, List<String> apiScopes, int timeoutMs) {
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CollabMacroResponse> list(UUID projectId, UUID documentId) {
        User user = currentUser();
        if (!permissionService.canReadInProject(projectId, user)) {
            throw new SecurityException("Bu projedeki makroları görüntüleme yetkiniz yok");
        }
        List<CollabMacro> macros = documentId != null
                ? macroRepository.findForDocument(projectId, documentId)
                : macroRepository.findByProjectIdAndDocumentIsNullOrderByNameAsc(projectId);
        return macros.stream()
                .map(macro -> CollabMacroResponse.from(macro, hash(macro.getSource())))
                .toList();
    }

    @Transactional
    public CollabMacroResponse create(UUID projectId, CollabMacroRequest request) {
        User user = currentUser();
        checkManageMacro(projectId, user);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
        entitlementService.assertFeature(project.getOrganization(), PlanFeature.COLLAB_MACRO);

        CollabMacro macro = CollabMacro.builder()
                .project(project)
                .document(resolveDocument(request.documentId(), projectId))
                .name(request.name().trim())
                .description(request.description())
                .source(request.source())
                .triggerType(request.triggerType() != null ? request.triggerType() : MacroTriggerType.MANUAL)
                .scheduleCron(request.scheduleCron())
                .enabled(request.enabled() == null || request.enabled())
                .apiScopes(scanner.scanToString(request.source()))
                .createdBy(user)
                .build();

        // Yeni makro **onaysız** doğar; yazarı bile onaylamış sayılmaz. Kendi
        // yazdığını otomatik onaylı kabul etmek, onay akışının tamamını atlatmanın
        // en kolay yolu olurdu.
        macro = macroRepository.save(macro);
        log.info("Makro oluşturuldu: {} (proje {})", macro.getName(), projectId);
        return CollabMacroResponse.from(macro, hash(macro.getSource()));
    }

    @Transactional
    public CollabMacroResponse update(UUID macroId, CollabMacroRequest request) {
        User user = currentUser();
        CollabMacro macro = require(macroId);
        checkManageMacro(macro.getProject().getId(), user);

        boolean sourceChanged = !macro.getSource().equals(request.source());

        macro.setName(request.name().trim());
        macro.setDescription(request.description());
        macro.setSource(request.source());
        macro.setTriggerType(request.triggerType() != null ? request.triggerType() : macro.getTriggerType());
        macro.setScheduleCron(request.scheduleCron());
        if (request.enabled() != null) macro.setEnabled(request.enabled());
        macro.setApiScopes(scanner.scanToString(request.source()));
        macro.setUpdatedAt(LocalDateTime.now());

        if (sourceChanged) {
            // Onay kaynağa değil kaynağın özetine verilmişti; kaynak değişince
            // düşer (§9.2). Aksi hâlde "onaylat, sonra değiştir" açık bir yol olurdu.
            macro.setApprovedAt(null);
            macro.setApprovedBy(null);
            macro.setSourceHash(null);
        }

        macroRepository.save(macro);
        return CollabMacroResponse.from(macro, hash(macro.getSource()));
    }

    @Transactional
    public void delete(UUID macroId) {
        User user = currentUser();
        CollabMacro macro = require(macroId);
        checkManageMacro(macro.getProject().getId(), user);
        macroRepository.delete(macro);
    }

    // ─── Onay akışı (§9.2) ───────────────────────────────────────────────────

    @Transactional
    public CollabMacroResponse approve(UUID macroId) {
        User user = currentUser();
        CollabMacro macro = require(macroId);
        checkManageMacro(macro.getProject().getId(), user);

        macro.setSourceHash(hash(macro.getSource()));
        macro.setApprovedBy(user);
        macro.setApprovedAt(LocalDateTime.now());
        macro.setUpdatedAt(LocalDateTime.now());
        macroRepository.save(macro);

        log.info("Makro onaylandı: {} ({})", macro.getName(), user.getEmail());
        return CollabMacroResponse.from(macro, macro.getSourceHash());
    }

    @Transactional
    public CollabMacroResponse revoke(UUID macroId) {
        User user = currentUser();
        CollabMacro macro = require(macroId);
        checkManageMacro(macro.getProject().getId(), user);

        macro.setApprovedAt(null);
        macro.setApprovedBy(null);
        macro.setSourceHash(null);
        macro.setUpdatedAt(LocalDateTime.now());
        macroRepository.save(macro);
        return CollabMacroResponse.from(macro, hash(macro.getSource()));
    }

    // ─── Çalıştırma ──────────────────────────────────────────────────────────

    /**
     * Çalıştırma iznini verir ve {@code RUNNING} kaydını açar.
     *
     * <p>Reddedilen denemeler de {@code DENIED} olarak yazılır: bir makronun kaç
     * kez yetkisiz çalıştırılmaya çalışıldığı, kaç kez çalıştığından daha
     * anlamlı bir sinyal olabilir.
     */
    @Transactional
    public RunTicket beginRun(UUID macroId, MacroTriggerType triggerType) {
        User user = currentUser();
        CollabMacro macro = require(macroId);
        UUID projectId = macro.getProject().getId();

        entitlementService.assertFeature(macro.getProject().getOrganization(), PlanFeature.COLLAB_MACRO);

        MacroTriggerType effective = triggerType != null ? triggerType : MacroTriggerType.MANUAL;
        String currentHash = hash(macro.getSource());
        boolean approved = macro.isApproved(currentHash);
        boolean isAuthor = macro.getCreatedBy() != null
                && macro.getCreatedBy().getEmail().equalsIgnoreCase(user.getEmail());

        String denial = denialReason(macro, user, projectId, effective, approved, isAuthor);
        if (denial != null) {
            recordRun(macro, user, effective, MacroRunStatus.DENIED, null, null, denial, false);
            throw new SecurityException(denial);
        }

        CollabMacroRun run = recordRun(macro, user, effective, MacroRunStatus.RUNNING,
                null, null, null, false);
        publishMacroEvent(macro, run, "started");

        return new RunTicket(
                run.getId(),
                macro.getSource(),
                macro.getApiScopes() == null || macro.getApiScopes().isBlank()
                        ? List.of() : List.of(macro.getApiScopes().split(",")),
                timeoutMsFor(macro));
    }

    private String denialReason(CollabMacro macro, User user, UUID projectId,
                                MacroTriggerType trigger, boolean approved, boolean isAuthor) {
        if (!Boolean.TRUE.equals(macro.getEnabled())) {
            return "Makro devre dışı.";
        }
        if (!permissionService.hasProjectPermission(projectId, user, Permission.COLLAB_RUN_MACRO)) {
            return "Makro çalıştırma yetkiniz yok.";
        }
        if (trigger != MacroTriggerType.MANUAL && !approved) {
            // Otomatik tetikleyicide "yazarı çalıştırabilir" istisnası yok:
            // ON_OPEN, dokümanı açan **herkesin** yetkisiyle sessizce koşar.
            return "Otomatik tetikleyiciler yalnızca onaylı makrolarda çalışır.";
        }
        if (!approved && !isAuthor) {
            return "Bu makro henüz onaylanmadı; yalnızca yazarı çalıştırabilir.";
        }
        return null;
    }

    /** PRO'da 30 sn, diğerlerinde 5 sn duvar saati (§9.2). */
    private int timeoutMsFor(CollabMacro macro) {
        String plan = macro.getProject().getOrganization() != null
                ? macro.getProject().getOrganization().getPlan() : null;
        return "FREE".equalsIgnoreCase(plan) ? 5_000 : 30_000;
    }

    @Transactional
    public void completeRun(UUID runId, CollabMacroRunReport report) {
        User user = currentUser();
        CollabMacroRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Çalıştırma kaydı bulunamadı"));

        // Kaydı yalnızca onu açan kullanıcı kapatabilir; aksi hâlde bir kullanıcı
        // başkasının çalıştırmasına sahte bir sonuç yazabilirdi.
        if (run.getTriggeredBy() == null
                || !run.getTriggeredBy().getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new SecurityException("Bu çalıştırma kaydı size ait değil");
        }
        if (run.getStatus() != MacroRunStatus.RUNNING) return;

        LocalDateTime finishedAt = LocalDateTime.now();
        run.setStatus(report.status() != null ? report.status() : MacroRunStatus.FAILED);
        run.setFinishedAt(finishedAt);
        run.setDurationMs(report.durationMs() != null
                ? report.durationMs()
                : Duration.between(run.getStartedAt(), finishedAt).toMillis());
        run.setLog(truncate(report.log()));
        run.setError(truncate(report.error()));
        run.setWroteDocument(Boolean.TRUE.equals(report.wroteDocument()));
        runRepository.save(run);

        publishMacroEvent(run.getMacro(), run, "finished");

        // Yalnızca yazan makrolar aktivite akışına düşer (§9.2): salt okuyan bir
        // makronun her çalıştırmasını akışa koymak, gerçek değişiklikleri
        // görünmez yapardı.
        if (Boolean.TRUE.equals(run.getWroteDocument()) && run.getStatus() == MacroRunStatus.SUCCESS) {
            CollabDocument document = run.getMacro().getDocument();
            activityService.record(
                    user.getEmail(),
                    ActivityAction.COLLAB_MACRO_RUN,
                    "CollabMacro",
                    run.getMacro().getId().toString(),
                    document != null && document.getTeam() != null ? document.getTeam().getId() : null,
                    Map.of("macro", run.getMacro().getName(),
                            "document", document != null ? document.getTitle() : "—"));
        }
    }

    @Transactional(readOnly = true)
    public List<CollabMacroRunResponse> runs(UUID macroId) {
        User user = currentUser();
        CollabMacro macro = require(macroId);
        if (!permissionService.canReadInProject(macro.getProject().getId(), user)) {
            throw new SecurityException("Bu makronun günlüğünü görüntüleme yetkiniz yok");
        }
        return runRepository.findByMacroIdOrderByStartedAtDesc(macroId, Limit.of(50))
                .stream()
                .map(CollabMacroRunResponse::from)
                .toList();
    }

    // ─── Yardımcılar ─────────────────────────────────────────────────────────

    private CollabMacroRun recordRun(CollabMacro macro, User user, MacroTriggerType trigger,
                                     MacroRunStatus status, Long durationMs, String log,
                                     String error, boolean wroteDocument) {
        return runRepository.save(CollabMacroRun.builder()
                .macro(macro)
                .triggeredBy(user)
                .triggerType(trigger)
                .status(status)
                .startedAt(LocalDateTime.now())
                .finishedAt(status == MacroRunStatus.RUNNING ? null : LocalDateTime.now())
                .durationMs(durationMs)
                .log(truncate(log))
                .error(truncate(error))
                .wroteDocument(wroteDocument)
                .build());
    }

    private void publishMacroEvent(CollabMacro macro, CollabMacroRun run, String phase) {
        CollabDocument document = macro.getDocument();
        if (document == null) return;
        messagingTemplate.convertAndSend(
                "/topic/collab/" + document.getId() + "/macro",
                Map.of("runId", run.getId().toString(),
                        "macro", macro.getName(),
                        "status", run.getStatus().name(),
                        "phase", phase));
    }

    private void checkManageMacro(UUID projectId, User user) {
        if (!permissionService.hasProjectPermission(projectId, user, Permission.COLLAB_MANAGE_MACRO)) {
            throw new SecurityException(
                    "Makro yazma ve onaylama için COLLAB_MANAGE_MACRO yetkisine ihtiyacınız var");
        }
    }

    private CollabDocument resolveDocument(UUID documentId, UUID projectId) {
        if (documentId == null) return null;
        CollabDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Doküman bulunamadı: " + documentId));
        if (!document.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Doküman bu projeye ait değil.");
        }
        return document;
    }

    private CollabMacro require(UUID macroId) {
        return macroRepository.findById(macroId)
                .orElseThrow(() -> new IllegalArgumentException("Makro bulunamadı: " + macroId));
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_LOG_CHARS ? value : value.substring(0, MAX_LOG_CHARS) + "\n… (kırpıldı)";
    }

    private static String hash(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 bulunamadı", e);
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı: " + email));
    }
}

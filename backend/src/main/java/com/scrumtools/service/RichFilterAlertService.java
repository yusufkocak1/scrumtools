package com.scrumtools.service;

import com.scrumtools.entity.RichFilter;
import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.enums.NotificationType;
import com.scrumtools.entity.enums.RichFilterElementKind;
import com.scrumtools.query.QueryContext;
import com.scrumtools.query.TaskQueryService;
import com.scrumtools.repository.RichFilterElementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Oran eşiği aşıldığında bildirim — panoya bakmayı gerektirmeyen tek özellik (Ö3).
 *
 * Pano pasif bir rapordur: kimse açmazsa "test aşamasındaki iş oranı %40'ı geçti"
 * bilgisi kimseye ulaşmaz. Uyarı kuralı bunu aktif sinyale çevirir.
 *
 * <h3>Neden bu, widget yapılandırmasında değil de tanımda?</h3>
 * Kuyruk ve oran <i>gösterimi</i> panoya ait (K21) — ama uyarı kimse bakmasa da
 * çalışmak zorunda, dolayısıyla sunucuda saklanan bir kurala ihtiyacı var. Bu yüzden
 * {@code RATIO} öğe türü burada, yalnız <b>uyarı kuralı</b> olarak kullanılıyor.
 *
 * <h3>Neden yalnız durum değişiminde bildirim?</h3>
 * Eşik aşılı kaldığı sürece her gün bildirim göndermek, bildirimi gürültüye çevirir
 * ve okunmaz hâle getirir. Kural yalnız <b>geçiş</b> anlarında konuşur: eşiğin altına
 * düşünce bir kez, normale dönünce bir kez. Durum öğenin config'inde
 * {@code _breached} altında taşınır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RichFilterAlertService {

    /** Uyarı durumunun config'te saklandığı ayrılmış anahtar. */
    private static final String STATE_KEY = "_breached";
    private static final String STATE_AT_KEY = "_breachedAt";

    private final RichFilterService richFilterService;
    private final RichFilterElementRepository elementRepository;
    private final RichFilterQueryFactory queryFactory;
    private final TaskQueryService taskQueryService;
    private final NotificationService notificationService;

    /**
     * Uyarı kurallarını günde bir kez değerlendirir.
     *
     * Sabah 09:00: gün içindeki ilk bakışta güncel olsun, gece yarısı bildirim
     * göndermesin. Bir kural patlarsa diğerleri değerlendirilmeye devam eder.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void evaluateAll() {
        List<RichFilterElement> rules =
                elementRepository.findByKindWithFilter(RichFilterElementKind.RATIO);

        for (RichFilterElement rule : rules) {
            try {
                evaluate(rule);
            } catch (Exception e) {
                log.warn("Oran uyarısı değerlendirilemedi (element={}): {}", rule.getId(), e.getMessage());
            }
        }
    }

    /**
     * Kuralı kullanıcı isteğiyle hesaplar — bildirim göndermez, durumu değiştirmez.
     *
     * Editördeki "Şimdi hesapla" bunu çağırır: kuralı kurarken sayıyı görmek için
     * bildirim tetiklemek, kuralı yazan kişiyi kendi denemeleriyle rahatsız ederdi.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> preview(UUID richFilterId, UUID elementId) {
        var user = richFilterService.currentUserOrThrow();
        RichFilter filter = richFilterService.requireAccessible(richFilterId, user);

        RichFilterElement rule = RichFilterQueryFactory
                .elementsOf(filter, RichFilterElementKind.RATIO).stream()
                .filter(e -> e.getId().equals(elementId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Uyarı kuralı bulunamadı: " + elementId));

        return compute(filter, rule).report();
    }

    /** Tek kuralı değerlendirir; durum değiştiyse bildirim gönderir. */
    @Transactional
    public Map<String, Object> evaluate(RichFilterElement rule) {
        RichFilter filter = rule.getRichFilter();
        Outcome outcome = compute(filter, rule);

        if (outcome.breached() != null) {
            boolean wasBreached = Boolean.TRUE.equals(rule.configOrEmpty().get(STATE_KEY));
            if (outcome.breached() != wasBreached) {
                notify(filter, rule, outcome);
                writeState(rule, outcome.breached());
            }
        }
        return outcome.report();
    }

    // ─── Hesaplama ────────────────────────────────────────────────────────────

    /**
     * Kuralın sonucu.
     *
     * @param breached null ise oran hesaplanamadı ({@code problem} dolu); bu durumda
     *                 bildirim gönderilmez — belirsizlik alarm sebebi değildir.
     */
    private record Outcome(RichFilterElement numerator, Long numeratorCount, Long denominatorCount,
                           Double ratio, double target, boolean higherBetter,
                           Boolean breached, String problem) {

        Map<String, Object> report() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("numerator", numeratorCount);
            out.put("denominator", denominatorCount);
            out.put("ratio", ratio);
            out.put("target", target);
            out.put("breached", breached);
            out.put("problem", problem);
            return out;
        }

        static Outcome problem(String message) {
            return new Outcome(null, null, null, null, 0, true, null, message);
        }
    }

    private Outcome compute(RichFilter filter, RichFilterElement rule) {
        Map<String, Object> config = rule.configOrEmpty();

        RichFilterElement numerator = queryFactory
                .smartFilterById(filter, config.get("numeratorId"))
                .orElse(null);
        if (numerator == null) {
            // Payı silinmiş kural sessizce çalışamaz; sahibinin düzeltmesi gerekir.
            return Outcome.problem("Pay olarak seçilen akıllı filtre silinmiş.");
        }
        RichFilterElement denominator = queryFactory
                .smartFilterById(filter, config.get("denominatorId"))
                .orElse(null);

        QueryContext ctx = queryFactory.contextOf(filter);
        long numeratorCount = taskQueryService.count(ctx, queryFactory.boundedBy(filter, numerator));
        long denominatorCount = taskQueryService.count(ctx, queryFactory.boundedBy(filter, denominator));

        double target = doubleValue(config.get("target"), 0);
        boolean higherBetter = !"lower_better".equals(String.valueOf(config.get("direction")));

        if (denominatorCount == 0) {
            // Payda sıfırken oran tanımsızdır; "0" sayıp uyarı göndermek yanlış alarm olurdu.
            return new Outcome(numerator, numeratorCount, 0L, null, target, higherBetter,
                    null, "Payda sıfır; oran hesaplanamadı.");
        }

        double ratio = (double) numeratorCount / denominatorCount;
        boolean breached = higherBetter ? ratio < target : ratio > target;

        return new Outcome(numerator, numeratorCount, denominatorCount, ratio, target,
                higherBetter, breached, null);
    }

    // ─── Bildirim ─────────────────────────────────────────────────────────────

    /**
     * Bildirim filtrenin sahibine gider.
     *
     * Takımın tamamına göndermek ilk bakışta cömert görünüyor ama uyarıyı kimin
     * kurduğunu bilmeyen kişilere gürültü olarak ulaşırdı. Sahibi kuralı kuran
     * kişidir; alıcı kümesini genişletmek, kural başına alıcı listesi tutmayı
     * gerektirir — talep gelirse eklenir.
     */
    private void notify(RichFilter filter, RichFilterElement rule, Outcome outcome) {
        RichFilterElement numerator = outcome.numerator();
        double ratio = outcome.ratio();
        double target = outcome.target();
        boolean breached = Boolean.TRUE.equals(outcome.breached());

        String percent = Math.round(ratio * 100) + "%";
        String targetText = Math.round(target * 100) + "%";
        String direction = outcome.higherBetter() ? "altına düştü" : "üzerine çıktı";

        String title = breached
                ? rule.getName() + ": eşik aşıldı"
                : rule.getName() + ": normale döndü";
        String message = breached
                ? "\"" + numerator.getName() + "\" oranı " + percent + " — hedefin (" + targetText + ") " + direction + "."
                : "\"" + numerator.getName() + "\" oranı " + percent + " ile hedefe (" + targetText + ") geri döndü.";

        Map<String, Object> data = new HashMap<>();
        data.put("teamId", filter.getTeam().getId().toString());
        data.put("richFilterId", filter.getId().toString());
        data.put("elementId", rule.getId().toString());
        data.put("ratio", ratio);
        data.put("target", target);
        data.put("breached", breached);

        notificationService.createAndPush(
                filter.getOwner().getEmail(),
                NotificationType.RICH_FILTER_ALERT,
                title,
                message,
                "rich_filter",
                filter.getId().toString(),
                data);

        log.info("Oran uyarısı: filtre='{}' kural='{}' oran={} hedef={} aşıldı={}",
                filter.getName(), rule.getName(), percent, targetText, breached);
    }

    /**
     * Durum öğenin config'ine yazılır.
     *
     * Kullanıcı kuralı düzenlerse config yeniden yazılır ve durum sıfırlanır; bunun
     * bedeli en fazla bir kez tekrar bildirimdir. Ayrı bir durum tablosu açmak, bu
     * kadarlık bir bilgi için fazlaydı.
     */
    private void writeState(RichFilterElement rule, boolean breached) {
        Map<String, Object> config = new LinkedHashMap<>(rule.configOrEmpty());
        config.put(STATE_KEY, breached);
        config.put(STATE_AT_KEY, LocalDateTime.now().toString());
        rule.setConfig(config);
        elementRepository.save(rule);
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private static double doubleValue(Object raw, double fallback) {
        if (raw instanceof Number number) return number.doubleValue();
        try {
            return raw == null ? fallback : Double.parseDouble(raw.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

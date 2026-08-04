package com.scrumtools.service;

import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.RichFilterElementKind;
import com.scrumtools.entity.enums.SeriesPointSource;
import com.scrumtools.query.*;
import com.scrumtools.repository.RichFilterElementRepository;
import com.scrumtools.repository.RichFilterSeriesPointRepository;
import com.scrumtools.repository.TaskHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Zaman serileri — bir akıllı filtrenin sayısının gün gün seyri.
 *
 * Sorgu motoru bu soruyu canlı cevaplayamaz: geçmişteki satır artık yok. İki
 * kaynaktan besleniyor (bkz. RICH_FILTER_PLAN.md — K13):
 *
 * <ul>
 *   <li><b>İleriye dönük:</b> gecelik iş o günkü değeri ölçüp yazar. Ölçülen
 *       değer sorgu motorunun kendi cevabıdır — grafiklerle aynı sayı.</li>
 *   <li><b>Geriye dönük:</b> {@code task_history} değişiklikleri ters uygulanarak
 *       son {@value #BACKFILL_DAYS} gün kurgulanır. Kurgu {@link SeriesReplay} ile
 *       yapılır ve <b>yalnız</b> tarihçede izlenen alanlara dayanan sorgular için
 *       mümkündür; değilse seri bugünden itibaren birikmeye başlar.</li>
 * </ul>
 *
 * Kurgu, yazmadan önce kendini denetler: bugünün kurgulanmış değeri SQL'in
 * verdiği değerle tutmuyorsa hiçbir nokta yazılmaz. Böylece iki değerlendirici
 * arasındaki olası bir semantik kayması yanlış geçmişe değil, eksik geçmişe
 * dönüşür.
 *
 * Serinin panodaki seçimden <b>etkilenmediğine</b> dikkat: noktalar öğe başına
 * önceden hesaplanır, çapraz filtreleme geçmişi yeniden hesaplayamaz (K23).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RichFilterSeriesService {

    /** Grafikte varsayılan olarak gösterilen gün sayısı. */
    public static final int DEFAULT_WINDOW_DAYS = 90;
    public static final int MAX_WINDOW_DAYS = 365;

    /** Geriye dönük kurgunun kapsadığı gün sayısı. */
    public static final int BACKFILL_DAYS = 180;

    /** Kurguda belleğe alınacak azami görev — üstünde kurgu reddedilir. */
    private static final int MAX_REPLAY_TASKS = 50_000;
    /** Kurguda işlenecek azami değişiklik kaydı. */
    private static final int MAX_REPLAY_EVENTS = 200_000;

    /** Tarihçedeki alan adı → STQL kanonik adı. */
    private static final Map<String, String> HISTORY_TO_FIELD = invert(SeriesReplay.REPLAYABLE_FIELDS);

    private final RichFilterService richFilterService;
    private final RichFilterElementRepository elementRepository;
    private final RichFilterSeriesPointRepository pointRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskQueryService taskQueryService;
    private final EntityManager em;

    // ─── Gecelik ölçüm ────────────────────────────────────────────────────────

    /**
     * Her zaman serisi öğesinin o günkü değerini yazar.
     *
     * Gece yarısından hemen sonra çalışır: gün bittiğinde o günün son hâli
     * ölçülmüş olur. Bir öğe patlarsa diğerleri yazılmaya devam eder — tek bozuk
     * sorgu bütün panoların geçmişini durdurmamalı.
     */
    @Scheduled(cron = "0 20 0 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void captureDaily() {
        LocalDate today = LocalDate.now();
        List<RichFilterElement> elements =
                elementRepository.findByKindWithFilter(RichFilterElementKind.TIME_SERIES);

        int written = 0;
        for (RichFilterElement element : elements) {
            try {
                RichFilter filter = element.getRichFilter();
                writePoint(element, today, measure(filter, element), SeriesPointSource.SNAPSHOT);
                written++;
            } catch (Exception e) {
                log.warn("Zaman serisi noktası yazılamadı (element={}): {}", element.getId(), e.getMessage());
            }
        }
        if (!elements.isEmpty()) {
            log.info("Zaman serisi anlık görüntüsü: {}/{} öğe yazıldı ({})", written, elements.size(), today);
        }
    }

    /** Öğenin bugünkü değeri — grafiklerle aynı sorgu motorundan. */
    private BigDecimal measure(RichFilter filter, RichFilterElement element) {
        long count = taskQueryService.count(contextOf(filter), seriesQuery(filter, element));
        return BigDecimal.valueOf(count);
    }

    private void writePoint(RichFilterElement element, LocalDate date, BigDecimal value,
                            SeriesPointSource source) {
        RichFilterSeriesPoint point = pointRepository
                .findByElementIdAndBucketDate(element.getId(), date)
                .orElseGet(() -> RichFilterSeriesPoint.builder()
                        .element(element)
                        .bucketDate(date)
                        .build());

        point.setValue(value);
        point.setSource(source);
        point.setComputedAt(LocalDateTime.now());
        pointRepository.save(point);
    }

    // ─── Okuma ────────────────────────────────────────────────────────────────

    /**
     * Zengin filtrenin bütün zaman serileri, tek istekte.
     *
     * Widget genelde birden çok seriyi aynı grafikte gösterir; öğe başına ayrı
     * istek, aynı pencerede N gidiş-dönüş demek olurdu.
     *
     * @param interval {@code "day"} | {@code "week"} — haftalık gösterimde haftanın
     *                 <b>son</b> değeri alınır, günlük değerler toplanmaz: seri bir
     *                 stok (o an açık olan görev sayısı), akış değil.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> series(UUID richFilterId, Integer days, String interval) {
        User user = richFilterService.currentUserOrThrow();
        RichFilter filter = richFilterService.requireAccessible(richFilterId, user);

        int window = normalizeWindow(days);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(window - 1L);
        boolean weekly = "week".equalsIgnoreCase(interval);

        List<Map<String, Object>> out = new ArrayList<>();
        for (RichFilterElement element : elementsOf(filter, RichFilterElementKind.TIME_SERIES)) {
            List<RichFilterSeriesPoint> points = pointRepository
                    .findByElementIdAndBucketDateBetweenOrderByBucketDateAsc(element.getId(), from, to);

            Map<String, Object> series = new LinkedHashMap<>();
            series.put("elementId", element.getId().toString());
            series.put("name", element.getName());
            series.put("color", colorOf(filter, element));
            series.put("smartFilterId", smartFilterIdOf(element).map(UUID::toString).orElse(null));
            series.put("from", from.toString());
            series.put("to", to.toString());
            series.put("interval", weekly ? "week" : "day");
            series.put("points", weekly ? weeklyPoints(points) : dailyPoints(points));
            out.add(series);
        }
        return out;
    }

    private static List<Map<String, Object>> dailyPoints(List<RichFilterSeriesPoint> points) {
        return points.stream().map(RichFilterSeriesService::point).toList();
    }

    /** Haftanın son ölçümü — hafta başlangıcı pazartesi. */
    private static List<Map<String, Object>> weeklyPoints(List<RichFilterSeriesPoint> points) {
        Map<LocalDate, RichFilterSeriesPoint> lastOfWeek = new LinkedHashMap<>();
        for (RichFilterSeriesPoint p : points) {
            LocalDate monday = p.getBucketDate().with(java.time.DayOfWeek.MONDAY);
            RichFilterSeriesPoint current = lastOfWeek.get(monday);
            if (current == null || p.getBucketDate().isAfter(current.getBucketDate())) {
                lastOfWeek.put(monday, p);
            }
        }
        return lastOfWeek.values().stream().map(RichFilterSeriesService::point).toList();
    }

    private static Map<String, Object> point(RichFilterSeriesPoint p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", p.getBucketDate().toString());
        map.put("value", p.getValue());
        map.put("source", p.getSource().name());
        return map;
    }

    private static int normalizeWindow(Integer days) {
        if (days == null || days <= 0) return DEFAULT_WINDOW_DAYS;
        return Math.min(days, MAX_WINDOW_DAYS);
    }

    // ─── Geriye dönük kurgu ───────────────────────────────────────────────────

    /**
     * Son {@value #BACKFILL_DAYS} günün değerlerini {@code task_history}'den kurgular.
     *
     * Sahibine açıktır: kurgu, tanımı değil türetilmiş veriyi yazar ama takımdaki
     * herkesin tetikleyebildiği ağır bir iş olmamalı.
     *
     * @return {@code {status, reason, written, from, to}} — status:
     *         {@code ok | unsupported | too_large | mismatch}
     */
    @Transactional
    public Map<String, Object> backfill(UUID richFilterId, UUID elementId) {
        User user = richFilterService.currentUserOrThrow();
        RichFilter filter = richFilterService.requireAccessible(richFilterId, user);
        if (!filter.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Geçmişi yalnızca zengin filtreyi oluşturan kişi kurgulayabilir.");
        }

        RichFilterElement element = elementsOf(filter, RichFilterElementKind.TIME_SERIES).stream()
                .filter(e -> e.getId().equals(elementId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Zaman serisi öğesi bulunamadı: " + elementId));

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(BACKFILL_DAYS - 1L);

        QueryNode replayNode;
        try {
            replayNode = replayNode(filter, element);
        } catch (UnsupportedReplayException e) {
            return outcome("unsupported", e.getMessage(), 0, start, today);
        }

        SeriesReplay.Support support = SeriesReplay.analyze(replayNode);
        if (!support.supported()) {
            return outcome("unsupported", support.reason(), 0, start, today);
        }

        QueryContext ctx = contextOf(filter);
        List<TaskRow> tasks = loadTasks(ctx);
        if (tasks.size() > MAX_REPLAY_TASKS) {
            return outcome("too_large",
                    "Kapsamda " + MAX_REPLAY_TASKS + "'den fazla görev var; geçmiş kurgusu bu ölçekte çalıştırılmıyor.",
                    0, start, today);
        }

        // Öz denetim: kurgunun bugünü, motorun bugünüyle aynı olmalı.
        long sqlToday = taskQueryService.count(ctx, seriesQuery(filter, element));
        long replayToday = tasks.stream()
                .filter(t -> SeriesReplay.matches(replayNode, t.snapshot(), ctx))
                .count();
        if (sqlToday != replayToday) {
            log.warn("Geçmiş kurgusu bugünü tutturamadı (element={}): sql={} replay={}",
                    element.getId(), sqlToday, replayToday);
            return outcome("mismatch",
                    "Kurgu bugünkü değeri doğrulayamadı; yanlış bir geçmiş yazmamak için hiçbir nokta kaydedilmedi.",
                    0, start, today);
        }

        List<Change> changes = loadChanges(filter, support.replayed(), start.atStartOfDay());
        if (changes.size() > MAX_REPLAY_EVENTS) {
            return outcome("too_large",
                    "Pencerede " + MAX_REPLAY_EVENTS + "'den fazla değişiklik var; geçmiş kurgusu çalıştırılmadı.",
                    0, start, today);
        }

        Map<LocalDate, BigDecimal> values = walkBackwards(tasks, changes, replayNode, ctx, start, today, replayToday);
        int written = persist(element, values, start, today, BigDecimal.valueOf(sqlToday));

        log.info("Zaman serisi geçmişi kurgulandı: element={} gün={} değişiklik={}",
                element.getId(), written, changes.size());
        return outcome("ok", null, written, start, today);
    }

    /**
     * Bugünden geriye yürüyerek gün gün değer üretir.
     *
     * Her gün için: o günün sonundan <b>sonra</b> olmuş değişiklikler ters uygulanır
     * ve o gün henüz oluşturulmamış görevler sayımdan düşer. Bütün görevleri her gün
     * yeniden değerlendirmek yerine yalnız durumu değişen görev yeniden değerlendirilir;
     * maliyet gün×görev değil, değişiklik sayısı kadardır.
     */
    private Map<LocalDate, BigDecimal> walkBackwards(List<TaskRow> tasks, List<Change> changes,
                                                     QueryNode node, QueryContext ctx,
                                                     LocalDate start, LocalDate today, long todayCount) {
        Map<UUID, TaskRow> byId = new HashMap<>();
        Map<UUID, Boolean> matched = new HashMap<>();
        Set<UUID> active = new HashSet<>();
        for (TaskRow row : tasks) {
            byId.put(row.id(), row);
            active.add(row.id());
            matched.put(row.id(), SeriesReplay.matches(node, row.snapshot(), ctx));
        }

        // Oluşturulma tarihine göre yeniden eskiye: geriye yürürken görevler
        // sırayla "henüz yok" durumuna düşer.
        List<TaskRow> byCreation = new ArrayList<>(tasks);
        // Tarihi bilinmeyen görev hiç düşmez: "ne zaman oluştuğu belirsiz" olanı
        // pencerenin dışına atmak, sayıyı sebepsiz küçültürdü.
        byCreation.sort(Comparator.comparing(TaskRow::createdAt,
                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())));

        Map<LocalDate, BigDecimal> values = new LinkedHashMap<>();
        values.put(today, BigDecimal.valueOf(todayCount));

        long count = todayCount;
        int changeIndex = 0;
        int creationIndex = 0;

        for (LocalDate day = today.minusDays(1); !day.isBefore(start); day = day.minusDays(1)) {
            LocalDateTime cutoff = day.atTime(LocalTime.MAX);

            while (changeIndex < changes.size() && changes.get(changeIndex).changedAt().isAfter(cutoff)) {
                Change change = changes.get(changeIndex++);
                TaskRow row = byId.get(change.taskId());
                String field = HISTORY_TO_FIELD.get(change.field());
                if (row == null || field == null) continue;

                row.snapshot().put(field, change.oldValue());
                boolean now = SeriesReplay.matches(node, row.snapshot(), ctx);
                boolean before = Boolean.TRUE.equals(matched.put(row.id(), now));
                if (active.contains(row.id()) && now != before) count += now ? 1 : -1;
            }

            while (creationIndex < byCreation.size()
                    && isAfter(byCreation.get(creationIndex).createdAt(), cutoff)) {
                TaskRow row = byCreation.get(creationIndex++);
                if (active.remove(row.id()) && Boolean.TRUE.equals(matched.get(row.id()))) count--;
            }

            values.put(day, BigDecimal.valueOf(count));
        }
        return values;
    }

    private static boolean isAfter(LocalDateTime moment, LocalDateTime cutoff) {
        return moment != null && moment.isAfter(cutoff);
    }

    /**
     * Kurgulanan günleri yazar.
     *
     * Önce eski kurgu silinir — yeniden kurgu, öncekinin üstüne eklemez. Ölçülmüş
     * (SNAPSHOT) noktalara dokunulmaz: onlar o gün fiilen sayıldı, kurgudan daha
     * güvenilirdir.
     */
    private int persist(RichFilterElement element, Map<LocalDate, BigDecimal> values,
                        LocalDate start, LocalDate today, BigDecimal todayValue) {
        pointRepository.deleteReplayedPoints(element.getId());

        Set<LocalDate> measured = new HashSet<>(pointRepository.findDates(element.getId(), start, today));

        int written = 0;
        for (Map.Entry<LocalDate, BigDecimal> entry : values.entrySet()) {
            if (entry.getKey().equals(today) || measured.contains(entry.getKey())) continue;
            writePoint(element, entry.getKey(), entry.getValue(), SeriesPointSource.REPLAY);
            written++;
        }

        // Bugün her hâlükârda ölçülmüş sayılır: değeri SQL'den geldi.
        writePoint(element, today, todayValue, SeriesPointSource.SNAPSHOT);
        return written;
    }

    // ─── Kurgu girdileri ──────────────────────────────────────────────────────

    /** Bellekte tutulan görev: kimlik, oluşturulma anı ve kurgulanabilir alanların değerleri. */
    private record TaskRow(UUID id, LocalDateTime createdAt, Map<String, String> snapshot) {
    }

    /** Tek bir alan değişikliği — ters uygulanacak. */
    private record Change(UUID taskId, String field, String oldValue, LocalDateTime changedAt) {
    }

    /**
     * Kapsamdaki görevlerin kurguya giren alanları.
     *
     * Kapsam kısıtı sorgu motorunun kendi {@code scopePredicates} çıktısıdır; takım
     * ve proje daraltması iki yerde ayrı ayrı yazılmasın.
     */
    private List<TaskRow> loadTasks(QueryContext ctx) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Task> root = query.from(Task.class);

        QueryPredicateBuilder builder = new QueryPredicateBuilder(cb, root, query, ctx);
        query.where(cb.and(builder.scopePredicates().toArray(new Predicate[0])));
        query.multiselect(
                root.get("id"), root.get("createdAt"),
                root.get("status"), root.get("priority"), root.get("assignee"),
                root.get("title"), root.get("customId"), root.get("reporter"));

        TypedQuery<Tuple> typed = em.createQuery(query);
        typed.setMaxResults(MAX_REPLAY_TASKS + 1);

        List<TaskRow> rows = new ArrayList<>();
        for (Tuple tuple : typed.getResultList()) {
            Map<String, String> snapshot = new HashMap<>();
            snapshot.put("status", (String) tuple.get(2));
            snapshot.put("priority", (String) tuple.get(3));
            snapshot.put("assignee", (String) tuple.get(4));
            snapshot.put("summary", (String) tuple.get(5));
            snapshot.put("key", (String) tuple.get(6));
            snapshot.put("reporter", (String) tuple.get(7));
            rows.add(new TaskRow((UUID) tuple.get(0), (LocalDateTime) tuple.get(1), snapshot));
        }
        return rows;
    }

    /** Pencere içindeki değişiklikler, yeniden eskiye. */
    private List<Change> loadChanges(RichFilter filter, Set<String> replayedFields, LocalDateTime from) {
        Set<String> historyFields = new HashSet<>();
        replayedFields.forEach(f -> historyFields.add(SeriesReplay.REPLAYABLE_FIELDS.get(f)));
        if (historyFields.isEmpty()) return List.of();

        UUID teamId = filter.getTeam().getId();
        Project project = filter.effectiveProject();
        PageRequest limit = PageRequest.of(0, MAX_REPLAY_EVENTS + 1);

        List<Object[]> rows = project == null
                ? taskHistoryRepository.findTeamChanges(teamId, historyFields, from, limit)
                : taskHistoryRepository.findProjectChanges(teamId, project.getId(), historyFields, from, limit);

        return rows.stream()
                .map(r -> new Change((UUID) r[0], (String) r[1], (String) r[2], (LocalDateTime) r[3]))
                .toList();
    }

    // ─── Sorgu kurulumu ───────────────────────────────────────────────────────

    /** Öğenin canlı sorgusu: temel sorgu AND bağlı akıllı filtrenin kovası. */
    private ParsedQuery seriesQuery(RichFilter filter, RichFilterElement element) {
        ParsedQuery base = QueryParser.parse(filter.effectiveBaseQuery());

        Optional<RichFilterElement> smart = boundSmartFilter(filter, element);
        if (smart.isEmpty()) return base;

        return QueryComposer.compose(base, List.of(List.of(
                QueryFragments.smartIn(filter.getName(), List.of(smart.get().getName())))));
    }

    /**
     * Kurgu için açılmış koşul ağacı.
     *
     * {@code smart[…]} koşulu bellekte çözülemez (sınıflandırma veri tabanında
     * yapılır), bu yüzden burada aynı semantikle elle açılır: <i>kural eşleşiyor
     * ve kendisinden önceki hiçbir kural eşleşmiyor</i> — {@code bucketPredicate}
     * ile birebir aynı kural.
     */
    private QueryNode replayNode(RichFilter filter, RichFilterElement element) {
        ParsedQuery base = QueryParser.parse(filter.effectiveBaseQuery());
        List<QueryNode> parts = new ArrayList<>();
        if (base.hasWhere()) parts.add(base.where());

        Optional<RichFilterElement> bound = boundSmartFilter(filter, element);
        if (bound.isPresent()) {
            parts.add(bucketNode(filter, bound.get()));
        }

        if (parts.isEmpty()) return null;
        return parts.size() == 1 ? parts.get(0) : new QueryNode.And(parts);
    }

    private QueryNode bucketNode(RichFilter filter, RichFilterElement target) {
        List<RichFilterElement> clauses = elementsOf(filter, RichFilterElementKind.SMART_FILTER);
        List<QueryNode> earlier = new ArrayList<>();
        QueryNode match = null;

        for (RichFilterElement clause : clauses) {
            ParsedQuery parsed = QueryParser.parse(clause.getQuery());
            if (!parsed.hasWhere()) {
                // Koşulsuz kural "her şey" demek; kendisinden sonrakileri boşaltır.
                throw new UnsupportedReplayException(
                        "'" + clause.getName() + "' akıllı filtresinin koşulu yok; sıralama kurgulanamaz.");
            }
            if (clause.getId().equals(target.getId())) {
                match = parsed.where();
                break;
            }
            earlier.add(parsed.where());
        }

        if (match == null) {
            throw new UnsupportedReplayException("Seriye bağlı akıllı filtre bulunamadı.");
        }
        if (earlier.isEmpty()) return match;

        QueryNode earlierNode = earlier.size() == 1 ? earlier.get(0) : new QueryNode.Or(earlier);
        return new QueryNode.And(List.of(match, new QueryNode.Not(earlierNode)));
    }

    /**
     * Öğenin bağlam nesnesi.
     *
     * Akıllı filtreler kayıt aramadan, elimizdeki zengin filtrenin kendi
     * öğelerinden çözülür: zamanlanmış iş oturum taşımadığı için görünürlük
     * denetimi yapan katalog {@code null} dönerdi. Kendi öğesine erişim zaten
     * tanımın parçası.
     */
    private QueryContext contextOf(RichFilter filter) {
        List<SmartClause> clauses = elementsOf(filter, RichFilterElementKind.SMART_FILTER).stream()
                .map(e -> new SmartClause(filter.getId(), e.getId(), e.getName(), e.getColor(),
                        QueryParser.parse(e.getQuery())))
                .toList();

        Project project = filter.effectiveProject();
        return taskQueryService.buildContext(
                filter.getTeam().getId(),
                project != null ? project.getId() : null,
                filter.getOwner().getEmail(),
                (_, name) -> name != null && name.equalsIgnoreCase(filter.getName()) ? clauses : null);
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private static Optional<UUID> smartFilterIdOf(RichFilterElement element) {
        Object raw = element.configOrEmpty().get("smartFilterId");
        if (raw == null || raw.toString().isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(raw.toString().trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<RichFilterElement> boundSmartFilter(RichFilter filter, RichFilterElement element) {
        return smartFilterIdOf(element).flatMap(id -> filter.getElements().stream()
                .filter(e -> e.getKind() == RichFilterElementKind.SMART_FILTER)
                .filter(e -> e.getId().equals(id))
                .findFirst());
    }

    /** Serinin rengi: kendi rengi yoksa bağlı akıllı filtrenin rengi. */
    private static String colorOf(RichFilter filter, RichFilterElement element) {
        if (element.getColor() != null && !element.getColor().isBlank()) return element.getColor();
        return boundSmartFilter(filter, element).map(RichFilterElement::getColor).orElse(null);
    }

    private static List<RichFilterElement> elementsOf(RichFilter filter, RichFilterElementKind kind) {
        return filter.getElements().stream()
                .filter(e -> e.getKind() == kind)
                .sorted(Comparator.comparingInt(e -> e.getPosition() == null ? 0 : e.getPosition()))
                .toList();
    }

    private static Map<String, Object> outcome(String status, String reason, int written,
                                               LocalDate from, LocalDate to) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("reason", reason);
        out.put("written", written);
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("days", (int) ChronoUnit.DAYS.between(from, to) + 1);
        return out;
    }

    private static Map<String, String> invert(Map<String, String> source) {
        Map<String, String> out = new HashMap<>();
        source.forEach((canonical, history) -> out.put(history, canonical));
        return Map.copyOf(out);
    }

    /** Kurgunun baştan mümkün olmadığı durumlar — kullanıcıya sebebiyle döner. */
    private static class UnsupportedReplayException extends RuntimeException {
        UnsupportedReplayException(String message) {
            super(message);
        }
    }
}

package com.scrumtools.service.workflow;

import com.scrumtools.entity.WorkflowStatus;
import com.scrumtools.entity.enums.StatusCategory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Bir takım/proje kapsamında geçerli durumların anlık görüntüsü.
 *
 * Rapor, sprint kapatma ve board gibi çok sayıda görev üzerinde dönen akışlar
 * durum başına veri tabanına gitmesin diye workflow bir kez çözülüp bu değer
 * nesnesine dönüştürülür.
 *
 * Katalogda karşılığı olmayan durum adları hata değildir: uygulama serbest metin
 * statüyü kabul etmeye devam ediyor (eski görevler, SCM/CI otomasyonu). Bu tür
 * adlar için kategori isimden tahmin edilir — {@link #categoryOf(String)}.
 */
public record TaskStatusCatalog(UUID workflowId, String workflowName, List<StatusView> statuses) {

    public record StatusView(
            UUID id,
            String name,
            StatusCategory category,
            String color,
            String icon,
            Integer position,
            Boolean isInitial,
            Boolean isFinal,
            Boolean isCancellation,
            String description
    ) {
        public static StatusView from(WorkflowStatus s) {
            return new StatusView(s.getId(), s.getName(), s.getCategory(), s.getColor(),
                    s.getIcon(), s.getPosition(), s.getIsInitial(), s.getIsFinal(),
                    s.getIsCancellation(), s.getDescription());
        }
    }

    /** Ad → durum eşlemesi; karşılaştırma büyük/küçük harf duyarsız. */
    private Map<String, StatusView> index() {
        Map<String, StatusView> map = new LinkedHashMap<>();
        for (StatusView s : statuses) {
            if (s.name() != null) map.put(s.name().toLowerCase(Locale.ROOT), s);
        }
        return map;
    }

    public StatusView find(String name) {
        if (name == null) return null;
        return index().get(name.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String name) {
        return find(name) != null;
    }

    /**
     * Durumun raporlama kategorisi. Katalogda yoksa addan tahmin edilir; böylece
     * workflow tanımlanmadan önce oluşmuş görevler de burndown/velocity'de doğru
     * tarafa düşer.
     */
    public StatusCategory categoryOf(String name) {
        StatusView view = find(name);
        return view != null ? view.category() : guessCategory(name);
    }

    /** İş bitti sayılıyor mu — kategori DONE ise ya da durum "final" işaretliyse. */
    public boolean isDone(String name) {
        StatusView view = find(name);
        if (view != null) {
            return view.category() == StatusCategory.DONE || Boolean.TRUE.equals(view.isFinal());
        }
        return guessCategory(name) == StatusCategory.DONE;
    }

    /**
     * İptal edilmiş iş mi. Katalogda yoksa yalnızca adı iptal çağrıştıran
     * değerler (Cancelled / İptal) böyle sayılır — sessiz yanlış pozitif olmasın.
     */
    public boolean isCancelled(String name) {
        StatusView view = find(name);
        if (view != null) return Boolean.TRUE.equals(view.isCancellation());
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("cancel") || n.contains("iptal");
    }

    /** Yeni görevlerin başlangıç durumu. */
    public String initialStatusName() {
        return statuses.stream()
                .filter(s -> Boolean.TRUE.equals(s.isInitial()))
                .map(StatusView::name)
                .findFirst()
                .orElseGet(() -> statuses.isEmpty() ? DefaultStatuses.FALLBACK_INITIAL : statuses.get(0).name());
    }

    /**
     * "Tamamlandı" aksiyonunun hedef durumu (sprint kapatırken yarım kalan işleri
     * toplu kapatma gibi). İptal durumları hariç tutulur.
     */
    public String doneStatusName() {
        return statuses.stream()
                .filter(s -> s.category() == StatusCategory.DONE)
                .filter(s -> !Boolean.TRUE.equals(s.isCancellation()))
                .map(StatusView::name)
                .findFirst()
                .orElse(DefaultStatuses.FALLBACK_DONE);
    }

    /** Görev listelerinde varsayılan olarak gizlenen durum adları. */
    public List<String> cancellationStatusNames() {
        List<String> names = statuses.stream()
                .filter(s -> Boolean.TRUE.equals(s.isCancellation()))
                .map(StatusView::name)
                .toList();
        return names.isEmpty() ? List.of(DefaultStatuses.FALLBACK_CANCELLED) : names;
    }

    public List<String> names() {
        return statuses.stream().map(StatusView::name).toList();
    }

    /**
     * Workflow'da tanımlı olmayan durum adları için isimden kategori tahmini.
     * Frontend'deki StatusBadge.guessCategoryFromName ile aynı sözcük kümesini
     * kullanır — iki tarafın aynı görevi farklı renklendirmemesi için.
     */
    public static StatusCategory guessCategory(String name) {
        if (name == null || name.isBlank()) return StatusCategory.TO_DO;
        String n = name.toLowerCase(Locale.ROOT);
        for (String token : List.of("done", "closed", "fixed", "verified", "cancelled", "canceled",
                "won't fix", "wont fix", "resolved", "tamamland", "iptal", "kapand")) {
            if (n.contains(token)) return StatusCategory.DONE;
        }
        for (String token : List.of("progress", "review", "testing", "test", "selected", "devam",
                "incelem", "geliştir", "gelistir")) {
            if (n.contains(token)) return StatusCategory.IN_PROGRESS;
        }
        return StatusCategory.TO_DO;
    }
}

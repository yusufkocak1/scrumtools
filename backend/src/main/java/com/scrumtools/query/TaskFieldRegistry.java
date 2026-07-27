package com.scrumtools.query;

import java.util.*;

/**
 * Sorgulanabilir Task alanlarının kataloğu — STQL'in tek doğruluk kaynağı.
 *
 * Hem çözümleyici doğrulaması, hem Criteria çevirisi, hem de arayüzün otomatik
 * tamamlama ucu buradan beslenir. Yeni bir alan sorgulanabilir yapılacaksa
 * tek değişiklik burada yapılır.
 *
 * Jira'dan gelen kullanıcı ezberi korunsun diye JQL adları (summary, key, type,
 * due, created) kanonik ad olarak seçilmiş, entity alan adları alias bırakılmıştır.
 */
public final class TaskFieldRegistry {

    /** cf[anahtar] söz diziminin ön eki. */
    public static final String CUSTOM_FIELD_PREFIX = "cf";

    private static final List<FieldDescriptor> FIELDS = List.of(
            FieldDescriptor.of("summary", "Başlık", FieldType.STRING, "title", "title"),
            FieldDescriptor.of("description", "Açıklama", FieldType.TEXT, "description", "desc"),
            FieldDescriptor.of("key", "Görev No", FieldType.STRING, "customId", "customId", "issuekey"),

            FieldDescriptor.of("status", "Durum", FieldType.ENUM, "status").withSuggest("statuses"),
            FieldDescriptor.of("priority", "Öncelik", FieldType.ENUM, "priority").withSuggest("priorities"),
            FieldDescriptor.of("type", "Tür", FieldType.ENUM, "issueType", "issueType", "issuetype")
                    .withSuggest("issueTypes"),
            FieldDescriptor.of("resolution", "Çözüm", FieldType.ENUM, "resolution").withSuggest("resolutions"),
            FieldDescriptor.of("environment", "Ortam", FieldType.STRING, "environment").withSuggest("environments"),

            FieldDescriptor.of("assignee", "Atanan", FieldType.USER, "assignee").withSuggest("users"),
            FieldDescriptor.of("reporter", "Açan", FieldType.USER, "reporter").withSuggest("users"),
            FieldDescriptor.of("developer", "Geliştirici", FieldType.USER, "developer").withSuggest("users"),
            FieldDescriptor.of("analyst", "Analist", FieldType.USER, "analyst").withSuggest("users"),
            FieldDescriptor.of("tester", "Test Eden", FieldType.USER, "tester").withSuggest("users"),

            FieldDescriptor.of("labels", "Etiket", FieldType.COLLECTION, "labels", "label")
                    .withSuggest("labels"),
            FieldDescriptor.of("watcher", "Takip Eden", FieldType.COLLECTION, "watchers", "watchers")
                    .withSuggest("users"),

            FieldDescriptor.of("sprint", "Sprint", FieldType.ENTITY_REF, "sprint", "sprintId")
                    .withRefNames("name").withSuggest("sprints"),
            FieldDescriptor.of("project", "Proje", FieldType.ENTITY_REF, "project", "projectId")
                    .withRefNames("key", "name").withSuggest("projects"),
            FieldDescriptor.of("release", "Sürüm", FieldType.ENTITY_REF, "release", "fixVersion", "releaseId")
                    .withRefNames("name").withSuggest("releases"),
            FieldDescriptor.of("parent", "Üst Görev", FieldType.ENTITY_REF, "parentTask", "parentTask")
                    .withRefNames("customId"),

            FieldDescriptor.of("storyPoints", "Story Point", FieldType.NUMBER, "storyPoints", "points"),
            FieldDescriptor.of("estimatedHours", "Tahmini Süre", FieldType.NUMBER, "estimatedHours", "estimate"),
            FieldDescriptor.of("loggedHours", "Harcanan Süre", FieldType.NUMBER, "loggedHours", "timeSpent"),
            FieldDescriptor.of("rank", "Sıra", FieldType.NUMBER, "position", "position"),

            FieldDescriptor.of("due", "Son Tarih", FieldType.DATE, "dueDate", "dueDate"),
            FieldDescriptor.of("startDate", "Başlangıç Tarihi", FieldType.DATE, "startDate"),
            FieldDescriptor.of("created", "Oluşturulma", FieldType.DATETIME, "createdAt", "createdAt"),
            FieldDescriptor.of("updated", "Güncellenme", FieldType.DATETIME, "updatedAt", "updatedAt"),
            FieldDescriptor.of("resolved", "Çözülme", FieldType.DATETIME, "resolvedAt", "resolvedAt")
    );

    /** Kanonik ad ve tüm alias'lar → tanım. Küçük harfe normalize edilmiş anahtarlar. */
    private static final Map<String, FieldDescriptor> LOOKUP = buildLookup();

    private TaskFieldRegistry() {
    }

    private static Map<String, FieldDescriptor> buildLookup() {
        Map<String, FieldDescriptor> map = new HashMap<>();
        for (FieldDescriptor f : FIELDS) {
            map.put(norm(f.name()), f);
            for (String alias : f.aliases()) {
                map.put(norm(alias), f);
            }
        }
        return Map.copyOf(map);
    }

    /** Katalogdaki tüm alanlar — arayüzün alan listesi ucu için. */
    public static List<FieldDescriptor> all() {
        return FIELDS;
    }

    /**
     * Alan adını çözer. cf[anahtar] biçimi dinamik olarak CUSTOM_FIELD tanımına dönüştürülür.
     * Bulunamazsa boş döner — çağıran hatayı konumuyla birlikte üretir.
     */
    public static Optional<FieldDescriptor> resolve(String rawName) {
        if (rawName == null || rawName.isBlank()) return Optional.empty();
        String name = rawName.trim();

        String customKey = customFieldKey(name);
        if (customKey != null) {
            return Optional.of(new FieldDescriptor(
                    CUSTOM_FIELD_PREFIX + "[" + customKey + "]",
                    Set.of(),
                    customKey,
                    FieldType.CUSTOM_FIELD,
                    customKey,
                    List.of(),
                    null));
        }
        return Optional.ofNullable(LOOKUP.get(norm(name)));
    }

    /** cf[environment] → "environment"; eşleşmezse null. */
    public static String customFieldKey(String name) {
        String n = name.trim();
        int open = n.indexOf('[');
        if (open <= 0 || !n.endsWith("]")) return null;
        if (!CUSTOM_FIELD_PREFIX.equalsIgnoreCase(n.substring(0, open))) return null;
        String key = n.substring(open + 1, n.length() - 1).trim();
        return key.isEmpty() ? null : key;
    }

    /** Yazım hatasında "şunu mu demek istediniz?" önerisi üretir. */
    public static Optional<String> suggestSimilar(String unknownField) {
        if (unknownField == null || unknownField.isBlank()) return Optional.empty();
        String target = norm(unknownField);
        return LOOKUP.keySet().stream()
                .filter(k -> k.startsWith(target.substring(0, 1)))
                .min(Comparator.comparingInt(k -> editDistance(k, target)))
                .filter(k -> editDistance(k, target) <= Math.max(2, target.length() / 3))
                .map(k -> LOOKUP.get(k).name());
    }

    private static String norm(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }

    /** Levenshtein mesafesi — yalnız öneri üretmek için, iki satırlık gezinme yeterli. */
    private static int editDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[b.length()];
    }
}

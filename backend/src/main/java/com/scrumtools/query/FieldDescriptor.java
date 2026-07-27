package com.scrumtools.query;

import java.util.List;
import java.util.Set;

/**
 * Sorgulanabilir bir Task alanının tanımı.
 *
 * @param name         STQL'deki kanonik ad (ör. "summary")
 * @param aliases      kabul edilen diğer yazımlar (ör. "title")
 * @param label        arayüzde gösterilecek Türkçe etiket
 * @param type         veri tipi — geçerli operatörleri belirler
 * @param path         Task entity'si üzerindeki JPA özellik adı (ör. "title", "sprint")
 * @param refNameField ENTITY_REF alanlarda UUID olmayan değerin eşleneceği alan(lar)
 * @param suggestSource otomatik tamamlamada değerlerin nereden geleceği; yoksa null
 */
public record FieldDescriptor(
        String name,
        Set<String> aliases,
        String label,
        FieldType type,
        String path,
        List<String> refNameField,
        String suggestSource
) {

    public static FieldDescriptor of(String name, String label, FieldType type, String path, String... aliases) {
        return new FieldDescriptor(name, Set.of(aliases), label, type, path, List.of(), null);
    }

    public FieldDescriptor withSuggest(String source) {
        return new FieldDescriptor(name, aliases, label, type, path, refNameField, source);
    }

    public FieldDescriptor withRefNames(String... fields) {
        return new FieldDescriptor(name, aliases, label, type, path, List.of(fields), suggestSource);
    }

    public boolean supports(QueryOperator op) {
        return type.supports(op);
    }
}

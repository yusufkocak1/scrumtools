package com.scrumtools.query;

import com.scrumtools.dto.TaskFilterCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eski {@code [{field, operator, values}]} filtre formatını STQL ağacına ve metnine çevirir.
 *
 * İki yönü de gerekli:
 *  - Ağaç, eski {@code POST /tasks/filter} ucunun yeni motorda çalışması için.
 *  - Metin, arayüzün "Basit" sekmesinde kurulan filtreyi "STQL" sekmesinde göstermesi için.
 *
 * Eski format yalnızca AND destekliyordu; çeviri de bu semantiği korur.
 */
public final class LegacyFilterTranslator {

    private LegacyFilterTranslator() {
    }

    /** Eski filtre listesi → AST. Boş liste null döner (koşulsuz sorgu). */
    public static QueryNode toNode(List<TaskFilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) return null;

        List<QueryNode> conditions = new ArrayList<>();
        for (TaskFilterCriteria f : filters) {
            QueryNode.Condition c = toCondition(f);
            if (c != null) conditions.add(c);
        }
        if (conditions.isEmpty()) return null;
        return conditions.size() == 1 ? conditions.get(0) : new QueryNode.And(conditions);
    }

    private static QueryNode.Condition toCondition(TaskFilterCriteria f) {
        if (f == null || f.getField() == null || f.getOperator() == null) return null;

        QueryOperator op = QueryOperator.fromLegacy(f.getOperator())
                .or(() -> QueryOperator.fromSymbol(f.getOperator()))
                .orElse(null);
        if (op == null) return null;

        // Eski istemciler sprintId gibi alan adları gönderiyor; katalog bunları alias olarak tanır.
        if (TaskFieldRegistry.resolve(f.getField()).isEmpty()) return null;

        List<QueryNode.ValueExpr> values = new ArrayList<>();
        if (!op.isUnary() && f.getValues() != null) {
            for (String v : f.getValues()) {
                if (v == null || v.isBlank()) continue;
                values.add(new QueryNode.Literal(v, QueryTokenType.STRING, 0, v.length()));
            }
            if (values.isEmpty()) return null;
        }
        return new QueryNode.Condition(f.getField(), op, values, 0, f.getField().length());
    }

    /** Eski filtre listesi → okunabilir STQL metni. Arayüzün sekme geçişinde kullanılır. */
    public static String toStql(List<TaskFilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) return "";

        List<String> parts = new ArrayList<>();
        for (TaskFilterCriteria f : filters) {
            QueryNode.Condition c = toCondition(f);
            if (c == null) continue;

            FieldDescriptor fd = TaskFieldRegistry.resolve(c.field()).orElse(null);
            String name = fd != null ? fd.name() : c.field();

            if (c.operator().isUnary()) {
                parts.add(name + " " + c.operator().symbol());
            } else if (c.operator().isMultiValue()) {
                String list = c.values().stream()
                        .map(v -> quote(((QueryNode.Literal) v).text()))
                        .reduce((a, b) -> a + ", " + b).orElse("");
                parts.add(name + " " + c.operator().symbol() + " (" + list + ")");
            } else {
                String value = quote(((QueryNode.Literal) c.values().get(0)).text());
                parts.add(name + " " + c.operator().symbol() + " " + value);
            }
        }
        return String.join(" AND ", parts);
    }

    /**
     * Değeri STQL'e uygun biçimde tırnaklar. Tırnaksız yazılabilecek kadar basit
     * değerler (harf/rakam) okunabilirlik için tırnaksız bırakılır.
     */
    static String quote(String raw) {
        if (raw == null) return "\"\"";
        boolean simple = !raw.isEmpty() && raw.chars().allMatch(ch ->
                Character.isLetterOrDigit(ch) || ch == '_' || ch == '-');
        boolean keyword = isReservedWord(raw);
        if (simple && !keyword) return raw;
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static boolean isReservedWord(String raw) {
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "AND", "OR", "NOT", "IN", "IS", "EMPTY", "NULL", "ORDER", "BY", "ASC", "DESC" -> true;
            default -> false;
        };
    }
}

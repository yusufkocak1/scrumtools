package com.scrumtools.service.ci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Job eşlemesindeki JSON parametre şablonundaki {{...}} yer tutucularını tetikleme
 * anındaki bağlamdan çözer. Sabit değerler olduğu gibi geçer; böylece müşterinin
 * mevcut Jenkins parametre adlarına uyum sağlanır, job tarafında değişiklik istenmez.
 *
 * <p>Şablon formatı: string değerli düz JSON nesnesi, örn.
 * {@code {"BRANCH":"{{branch}}","TASK":"{{taskKey}}","ENV":"test"}}.
 */
@Component
public class CiParameterResolver {

    /** {{  degisken  }} — iç boşluklara toleranslı. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z]+)\\s*}}");

    /** Şablonda kullanılabilecek değişken adları — hata mesajlarında da listelenir. */
    public static final Set<String> SUPPORTED_VARIABLES = Set.of(
            "branch", "taskKey", "taskTitle", "releaseName", "projectKey", "triggeredBy");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Şablonu yapısal olarak doğrular (eşleme kaydedilirken çağrılır): geçerli JSON nesnesi mi,
     * değerler string mi ve yalnız bilinen değişkenlere mi atıf var. Boş/null şablon = parametresiz job.
     * Bağlam gerektirmez; bilinmeyen değişkeni burada erkenden yakalar.
     */
    public void validateTemplate(String template) {
        if (isBlank(template)) return;
        Map<String, String> raw = parse(template);
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            Matcher matcher = PLACEHOLDER.matcher(entry.getValue());
            while (matcher.find()) {
                String variable = matcher.group(1);
                if (!SUPPORTED_VARIABLES.contains(variable)) {
                    throw new IllegalArgumentException("Bilinmeyen şablon değişkeni: {{" + variable
                            + "}} (parametre '" + entry.getKey() + "'). Kullanılabilir değişkenler: "
                            + sortedSupported() + ".");
                }
            }
        }
    }

    /**
     * Şablonu verilen bağlamla çözer.
     * @return parametre adı → çözülmüş değer (şablon boşsa boş harita)
     * @throws IllegalArgumentException bilinmeyen değişken ya da bu bağlamda değeri olmayan değişkende
     */
    public Map<String, String> resolve(String template, CiParameterContext context) {
        Map<String, String> resolved = new LinkedHashMap<>();
        if (isBlank(template)) return resolved;

        Map<String, String> variables = context.asVariableMap();
        for (Map.Entry<String, String> entry : parse(template).entrySet()) {
            resolved.put(entry.getKey(), substitute(entry.getKey(), entry.getValue(), variables));
        }
        return resolved;
    }

    private String substitute(String paramName, String rawValue, Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER.matcher(rawValue);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (!SUPPORTED_VARIABLES.contains(variable)) {
                throw new IllegalArgumentException("Bilinmeyen şablon değişkeni: {{" + variable
                        + "}} (parametre '" + paramName + "'). Kullanılabilir değişkenler: "
                        + sortedSupported() + ".");
            }
            String value = variables.get(variable);
            if (value == null) {
                throw new IllegalArgumentException("'{{" + variable + "}}' değişkeni bu tetikleme "
                        + "bağlamında kullanılamıyor (parametre '" + paramName + "'). "
                        + "Örneğin release adı yalnız release pipeline'ında, branch yalnız task deploy'unda mevcuttur.");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Map<String, String> parse(String template) {
        try {
            Map<String, Object> raw = objectMapper.readValue(template, new TypeReference<>() {});
            Map<String, String> stringified = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    throw new IllegalArgumentException("Parametre '" + entry.getKey()
                            + "' için değer boş olamaz.");
                }
                if (value instanceof Map || value instanceof java.util.List) {
                    throw new IllegalArgumentException("Parametre '" + entry.getKey()
                            + "' değeri düz bir metin olmalı (iç içe nesne/dizi desteklenmez).");
                }
                stringified.put(entry.getKey(), String.valueOf(value));
            }
            return stringified;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Parametre şablonu geçerli bir JSON nesnesi değil.");
        }
    }

    private String sortedSupported() {
        return SUPPORTED_VARIABLES.stream().sorted()
                .map(v -> "{{" + v + "}}")
                .reduce((a, b) -> a + ", " + b).orElse("");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

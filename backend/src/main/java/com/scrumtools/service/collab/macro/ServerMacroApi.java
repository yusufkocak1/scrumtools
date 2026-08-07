package com.scrumtools.service.collab.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumtools.dto.DocPageRequest;
import com.scrumtools.query.TaskQueryService;
import com.scrumtools.service.DocPageService;
import com.scrumtools.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sunucu makrosunun gördüğü {@code ScrumTools} nesnesi (plan §9.1).
 *
 * <p><b>Tarayıcı sürümünden iki farkı var ve ikisi de yapısal:</b>
 *
 * <ol>
 *   <li><b>Doküman API'si yok.</b> Sunucuda Yjs yok (K2) — {@code Y.Doc} açmadan
 *       bir hücreyi okumak da yazmak da mümkün değil. Sunucu makrosu ScrumTools
 *       verisiyle çalışır, ortak dokümana dokunamaz. Bunu "sonra ekleriz" diye
 *       bırakmak yerine açıkça reddetmek gerekiyor: yarım çalışan bir doküman
 *       API'si, sessizce hiçbir şey yapmayan makrolar üretirdi.</li>
 *   <li><b>Kimlik onaylayanındır.</b> Tarayıcıda makro çalıştıranın oturumunu
 *       kullanır; burada çalıştıran bir insan yok. Bağlam
 *       {@link ServerMacroRunner} tarafından kurulur, bu sınıf yalnızca mevcut
 *       servisleri çağırır — yani ayrı bir yetki yolu yine açılmıyor (Faz 4
 *       kararının devamı).</li>
 * </ol>
 *
 * <p><b>Sınırdan Java nesnesi geçmez.</b> Sonuçlar JSON'a çevrilip
 * {@code JSON.parse} ile JS tarafına veriliyor. Böylece {@code HostAccess.NONE}
 * bozulmadan kalıyor: betik hiçbir Java metoduna erişemez, elindeki şey düz veri.
 */
@Component
@RequiredArgsConstructor
public class ServerMacroApi {

    private final ObjectMapper objectMapper;
    private final TaskQueryService taskQueryService;
    private final SprintService sprintService;
    private final DocPageService docPageService;
    private final MacroHttpProxyService httpProxyService;

    /**
     * Çalıştırma boyunca biriken günlük ve tetikleyici verisi.
     *
     * <p>Entity değil <b>kimlikler</b> taşınıyor: yürütme kuyruk iş parçacığında
     * ve açık bir işlem dışında olduğu için, tembel bir ilişkiye dokunmak
     * {@code LazyInitializationException} verirdi.
     */
    public record RunContext(UUID projectId, Map<String, Object> triggerPayload,
                             StringBuilder log) {
    }

    /** Günlüğün üst sınırı — makro sonsuz satır basabilir. */
    private static final int MAX_LOG_CHARS = 20_000;

    /**
     * {@code ScrumTools} ve {@code console} global'lerini bağlama kurar.
     */
    public void install(Context context, RunContext run) {
        Value bindings = context.getBindings("js");
        Value jsonParse = context.eval("js", "JSON.parse");
        Value jsonStringify = context.eval("js", "JSON.stringify");

        bindings.putMember("ScrumTools", scrumTools(run, jsonParse, jsonStringify));
        bindings.putMember("console", console(run));
    }

    private ProxyObject scrumTools(RunContext run, Value jsonParse, Value jsonStringify) {
        Map<String, Object> root = new HashMap<>();

        root.put("tasks", ProxyObject.fromMap(Map.of(
                "query", (ProxyExecutable) args -> {
                    String tql = str(args, 0);
                    Map<String, Object> options = obj(args, 1, jsonStringify);
                    UUID teamId = uuid(options.get("teamId"));
                    if (teamId == null) {
                        throw new IllegalArgumentException(
                                "ScrumTools.tasks.query için { teamId } gerekli: sunucu makrosunda "
                                        + "açık bir doküman yok, takım kendiliğinden bilinemez.");
                    }
                    return toJs(jsonParse, taskQueryService.search(
                            teamId,
                            uuid(options.get("projectId")),
                            tql,
                            intOr(options.get("page"), 0),
                            intOr(options.get("size"), 100)));
                })));

        root.put("sprints", ProxyObject.fromMap(Map.of(
                "current", (ProxyExecutable) args -> {
                    UUID teamId = uuid(str(args, 0));
                    if (teamId == null) {
                        throw new IllegalArgumentException("ScrumTools.sprints.current(teamId) gerekli.");
                    }
                    // Ayrı bir "aktif sprint" ucu yok; liste üzerinden seçiliyor —
                    // tarayıcı sürümüyle aynı davranış.
                    Object active = sprintService.getSprintsByTeam(teamId).stream()
                            .filter(sprint -> "ACTIVE".equalsIgnoreCase(String.valueOf(
                                    readField(sprint, "status"))))
                            .findFirst()
                            .orElse(null);
                    return toJs(jsonParse, active);
                })));

        root.put("docs", ProxyObject.fromMap(Map.of(
                "getPage", (ProxyExecutable) args -> {
                    UUID pageId = uuid(str(args, 0));
                    if (pageId == null) throw new IllegalArgumentException("ScrumTools.docs.getPage(pageId) gerekli.");
                    return toJs(jsonParse, docPageService.getPage(pageId));
                },
                "savePage", (ProxyExecutable) args -> {
                    UUID spaceId = uuid(str(args, 0));
                    if (spaceId == null) throw new IllegalArgumentException("ScrumTools.docs.savePage(spaceId, …) gerekli.");
                    DocPageRequest request = new DocPageRequest(
                            str(args, 1), str(args, 2), null, null, "Sunucu makrosu");
                    return toJs(jsonParse, docPageService.createPage(spaceId, request));
                })));

        root.put("http", ProxyObject.fromMap(Map.of(
                "fetch", (ProxyExecutable) args -> {
                    Map<String, Object> options = obj(args, 1, jsonStringify);
                    return toJs(jsonParse, httpProxyService.fetch(
                            run.projectId(),
                            new MacroHttpProxyService.ProxyRequest(
                                    str(args, 0),
                                    options.get("method") == null ? null : String.valueOf(options.get("method")),
                                    castHeaders(options.get("headers")),
                                    options.get("body") == null ? null : String.valueOf(options.get("body")))));
                })));

        // Tetikleyici verisi: webhook gövdesi buradan okunur. Salt veri, dolayısıyla
        // doğrudan JSON olarak geçiyor.
        root.put("trigger", toJs(jsonParse, run.triggerPayload() == null
                ? Map.of() : run.triggerPayload()));

        // Doküman API'si bilerek yok — sessizce `undefined` bırakmak yerine
        // sebebini söyleyen bir hata veriyor.
        root.put("getActiveDocument", (ProxyExecutable) args -> {
            throw new UnsupportedOperationException(
                    "Sunucu makrosu ortak dokümana erişemez: sunucu Yjs durumunu açmaz (plan K2). "
                            + "Doküman okuyan/yazan makrolar tarayıcıda MANUAL/ON_OPEN/ON_EDIT ile çalışmalı.");
        });

        return ProxyObject.fromMap(root);
    }

    private ProxyObject console(RunContext run) {
        ProxyExecutable write = args -> {
            List<String> parts = new ArrayList<>();
            for (Value arg : args) parts.add(arg == null ? "null" : arg.toString());
            append(run.log(), String.join(" ", parts));
            return null;
        };
        return ProxyObject.fromMap(Map.of(
                "log", write, "info", write, "warn", write, "error", write, "debug", write));
    }

    private void append(StringBuilder log, String line) {
        if (log.length() >= MAX_LOG_CHARS) return;
        log.append(line).append('\n');
        if (log.length() >= MAX_LOG_CHARS) log.append("… (kırpıldı)\n");
    }

    // ─── Sınır dönüşümleri ───────────────────────────────────────────────────

    /** Java sonucu → JS değeri, JSON üzerinden (host nesnesi sızmasın). */
    private Value toJs(Value jsonParse, Object value) {
        try {
            return jsonParse.execute(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            throw new IllegalStateException("Sonuç makroya aktarılamadı: " + e.getMessage(), e);
        }
    }

    private String str(Value[] args, int index) {
        if (args.length <= index || args[index] == null || args[index].isNull()) return null;
        return args[index].isString() ? args[index].asString() : args[index].toString();
    }

    /** JS nesnesi → Java {@code Map}, yine JSON üzerinden. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> obj(Value[] args, int index, Value jsonStringify) {
        if (args.length <= index || args[index] == null || args[index].isNull()) return Map.of();
        try {
            Value json = jsonStringify.execute(args[index]);
            if (json == null || json.isNull()) return Map.of();
            return objectMapper.readValue(json.asString(), Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Seçenek nesnesi okunamadı: " + e.getMessage());
        }
    }

    private Map<String, String> castHeaders(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) return null;
        Map<String, String> headers = new HashMap<>();
        map.forEach((key, value) -> headers.put(String.valueOf(key), String.valueOf(value)));
        return headers;
    }

    private UUID uuid(Object raw) {
        if (raw == null) return null;
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int intOr(Object raw, int fallback) {
        if (raw instanceof Number number) return number.intValue();
        try {
            return raw == null ? fallback : Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** DTO'lar record ya da POJO olabildiği için alan okuma JSON üzerinden yapılır. */
    private Object readField(Object dto, String field) {
        try {
            return objectMapper.convertValue(dto, Map.class).get(field);
        } catch (Exception e) {
            return null;
        }
    }
}

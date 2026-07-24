package com.scrumtools.service.ci.client;

import com.scrumtools.entity.enums.CiBuildStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Jenkins REST API istemcisi.
 *
 * <p>Auth: Basic (kullanıcı adı + API token). API token kullanıldığında CSRF crumb
 * çoğu kurulumda gerekmez; zorunlu kılan yapılandırmalar için crumb otomatik alınır.
 *
 * <p><b>URL yeniden çapalama:</b> Jenkins yanıtlarında dönen mutlak URL'ler kendi
 * "Jenkins URL" ayarına göre üretilir ve bizim baseUrl'imizden farklı (çoğu zaman iç ağdaki)
 * bir host içerebilir. Bu URL'lere doğrudan istek atmak hem kırılgan hem de SSRF açığıdır —
 * bu yüzden dönen her URL'in yalnız yol kısmı alınıp doğrulanmış baseUrl üzerine çapalanır.
 */
public class JenkinsClient implements CiClient {

    private static final String PROVIDER = "Jenkins";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    /** Folder keşfinde inilecek en fazla derinlik (plan bölüm 5.2). */
    private static final int MAX_FOLDER_DEPTH = 3;

    private final String baseUrl;
    private final RestClient restClient;
    private final boolean crumbEnabled;

    public JenkinsClient(String baseUrl, String username, String token, boolean crumbEnabled) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.crumbEnabled = crumbEnabled;

        String basic = Base64.getEncoder().encodeToString(
                (username + ":" + token).getBytes(StandardCharsets.UTF_8));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ─── Bağlantı testi ───────────────────────────────────────────────────────

    @Override
    public CiServerInfo validateConnection() {
        try {
            ResponseEntity<Map<String, Object>> response = restClient.get()
                    .uri("/api/json?tree=nodeName")
                    .retrieve()
                    .toEntity(MAP_TYPE);

            // Kimlik doğrulanmış kullanıcıyı ayrıca sorgularız; başarısız olursa test yine geçerli
            String user = fetchCurrentUser();
            return new CiServerInfo(
                    response.getHeaders().getFirst("X-Jenkins"),
                    user,
                    detectCrumbRequired());
        } catch (RestClientResponseException e) {
            throw wrap(e, "bağlantı testi");
        } catch (ResourceAccessException e) {
            throw unreachable(e, "bağlantı testi");
        }
    }

    private String fetchCurrentUser() {
        try {
            Map<String, Object> me = restClient.get()
                    .uri("/me/api/json?tree=id,fullName")
                    .retrieve().body(MAP_TYPE);
            if (me == null) return null;
            Object id = me.get("id");
            return id != null ? String.valueOf(id) : str(me.get("fullName"));
        } catch (Exception e) {
            return null; // /me kapalı olabilir — testin sonucunu etkilemez
        }
    }

    /** crumbIssuer 200 dönüyorsa kurulum CSRF koruması açık demektir; 404 = kapalı. */
    private boolean detectCrumbRequired() {
        try {
            restClient.get().uri("/crumbIssuer/api/json").retrieve().body(MAP_TYPE);
            return true;
        } catch (RestClientResponseException e) {
            return false;
        } catch (ResourceAccessException e) {
            return false;
        }
    }

    // ─── Job keşfi ────────────────────────────────────────────────────────────

    @Override
    public List<CiJobInfo> listJobs(String search) {
        try {
            Map<String, Object> root = restClient.get()
                    .uri("/api/json?tree=" + jobsTree(MAX_FOLDER_DEPTH))
                    .retrieve().body(MAP_TYPE);

            List<CiJobInfo> collected = new ArrayList<>();
            collectJobs(root == null ? Map.of() : root, collected);

            String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            return collected.stream()
                    .filter(j -> q.isEmpty() || j.fullName().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        } catch (RestClientResponseException e) {
            throw wrap(e, "job listeleme");
        } catch (ResourceAccessException e) {
            throw unreachable(e, "job listeleme");
        }
    }

    /** tree=jobs[fullName,...,jobs[fullName,...]] — folder'ları verilen derinliğe kadar açar. */
    private String jobsTree(int depth) {
        String leaf = "jobs[fullName,displayName,url,buildable,_class]";
        String tree = leaf;
        for (int i = 1; i < depth; i++) {
            tree = leaf.substring(0, leaf.length() - 1) + "," + tree + "]";
        }
        return tree;
    }

    /** Folder düğümlerinde recurse eder, yalnız tetiklenebilir job'ları toplar. */
    @SuppressWarnings("unchecked")
    private void collectJobs(Map<String, Object> node, List<CiJobInfo> out) {
        Object jobs = node.get("jobs");
        if (!(jobs instanceof List<?> list)) return;

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) continue;
            Map<String, Object> job = (Map<String, Object>) raw;

            if (isContainer(job)) {
                collectJobs(job, out); // folder / multibranch — çocukları taranır, kendisi eşlenemez
                continue;
            }
            String fullName = str(job.get("fullName"));
            if (fullName == null) continue;

            out.add(new CiJobInfo(
                    fullName,
                    job.get("displayName") != null ? str(job.get("displayName")) : fullName,
                    reanchor(str(job.get("url")), "/job/"),
                    Boolean.TRUE.equals(job.get("buildable"))));
        }
    }

    /**
     * Folder / multibranch düğümü mü. "jobs" alanı gelmişse kesin container'dır;
     * en derin seviyede alt job'lar sorgulanmadığı için _class'a da bakılır —
     * aksi halde folder'lar tetiklenemez birer job gibi listeye düşerdi.
     */
    private boolean isContainer(Map<String, Object> job) {
        if (job.containsKey("jobs")) return true;
        String clazz = str(job.get("_class"));
        if (clazz == null) return false;
        String lower = clazz.toLowerCase(Locale.ROOT);
        return lower.contains("folder") || lower.contains("multibranch");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CiJobParameter> getJobParameters(String jobFullName) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(jobPath(jobFullName) + "/api/json?tree=property[parameterDefinitions"
                            + "[name,type,choices,defaultParameterValue[value]]]")
                    .retrieve().body(MAP_TYPE);

            List<CiJobParameter> parameters = new ArrayList<>();
            if (response == null) return parameters;

            for (Object prop : asList(response.get("property"))) {
                if (!(prop instanceof Map<?, ?> p)) continue;
                for (Object def : asList(((Map<String, Object>) p).get("parameterDefinitions"))) {
                    if (!(def instanceof Map<?, ?> d)) continue;
                    Map<String, Object> definition = (Map<String, Object>) d;
                    parameters.add(new CiJobParameter(
                            str(definition.get("name")),
                            str(definition.get("type")),
                            str(asMap(definition.get("defaultParameterValue")).get("value")),
                            asList(definition.get("choices")).stream().map(String::valueOf).toList()));
                }
            }
            return parameters;
        } catch (RestClientResponseException e) {
            throw wrap(e, "job parametrelerini okuma");
        } catch (ResourceAccessException e) {
            throw unreachable(e, "job parametrelerini okuma");
        }
    }

    // ─── Tetikleme ve durum ───────────────────────────────────────────────────

    @Override
    public String triggerBuild(String jobFullName, Map<String, String> parameters) {
        boolean parameterized = parameters != null && !parameters.isEmpty();
        String endpoint = jobPath(jobFullName) + (parameterized ? "/buildWithParameters" : "/build");

        try {
            RestClient.RequestBodySpec request = restClient.post().uri(endpoint);
            addCrumbIfNeeded(request);

            ResponseEntity<Void> response;
            if (parameterized) {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                parameters.forEach(form::add);
                response = request
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve().toBodilessEntity();
            } else {
                response = request.retrieve().toBodilessEntity();
            }

            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            String queueUrl = reanchor(location, "/queue/item/");
            if (queueUrl == null) {
                throw new CiApiException(502, PROVIDER + " tetikleme yanıtında kuyruk adresi yok — "
                        + "job tetiklenmiş olabilir, Jenkins arayüzünden kontrol edin.");
            }
            return queueUrl;
        } catch (RestClientResponseException e) {
            throw wrap(e, "build tetikleme");
        } catch (ResourceAccessException e) {
            throw unreachable(e, "build tetikleme");
        }
    }

    @Override
    public CiQueueStatus getQueueStatus(String queueItemUrl) {
        String path = relativize(queueItemUrl, "/queue/item/");
        try {
            Map<String, Object> item = restClient.get()
                    .uri(path + "api/json?tree=cancelled,why,executable[number,url]")
                    .retrieve().body(MAP_TYPE);
            if (item == null) return new CiQueueStatus(null, null, false, null);

            Map<String, Object> executable = asMap(item.get("executable"));
            Integer number = executable.get("number") instanceof Number n ? n.intValue() : null;

            return new CiQueueStatus(
                    number,
                    number == null ? null : reanchor(str(executable.get("url")), "/job/"),
                    Boolean.TRUE.equals(item.get("cancelled")),
                    str(item.get("why")));
        } catch (RestClientResponseException e) {
            // Jenkins kuyruk kayıtlarını ~5 dk sonra düşürür; 404 = kayıp
            if (e.getStatusCode().value() == 404) {
                throw new CiApiException(404, PROVIDER + " kuyruk kaydı bulunamadı (süresi dolmuş olabilir).");
            }
            throw wrap(e, "kuyruk durumu sorgulama");
        } catch (ResourceAccessException e) {
            throw unreachable(e, "kuyruk durumu sorgulama");
        }
    }

    @Override
    public CiBuildInfo getBuildInfo(String buildUrl) {
        String path = relativize(buildUrl, "/job/");
        try {
            Map<String, Object> build = restClient.get()
                    .uri(path + "api/json?tree=building,result,timestamp,duration")
                    .retrieve().body(MAP_TYPE);
            if (build == null) {
                throw new CiApiException(502, PROVIDER + " build durumu okunamadı.");
            }

            boolean building = Boolean.TRUE.equals(build.get("building"));
            return new CiBuildInfo(
                    building ? CiBuildStatus.RUNNING : mapResult(str(build.get("result"))),
                    build.get("timestamp") instanceof Number t ? t.longValue() : null,
                    build.get("duration") instanceof Number d ? d.longValue() : null);
        } catch (RestClientResponseException e) {
            throw wrap(e, "build durumu sorgulama");
        } catch (ResourceAccessException e) {
            throw unreachable(e, "build durumu sorgulama");
        }
    }

    /** result null ise build bitmiş ama sonucu henüz yazılmamıştır — RUNNING'de kalır. */
    private CiBuildStatus mapResult(String result) {
        if (result == null) return CiBuildStatus.RUNNING;
        return switch (result.toUpperCase(Locale.ROOT)) {
            case "SUCCESS" -> CiBuildStatus.SUCCESS;
            case "UNSTABLE" -> CiBuildStatus.UNSTABLE;
            case "ABORTED", "NOT_BUILT" -> CiBuildStatus.ABORTED;
            default -> CiBuildStatus.FAILURE;
        };
    }

    // ─── CSRF crumb ───────────────────────────────────────────────────────────

    /** Crumb yalnız bağlantıda işaretliyse alınır — gereksiz her POST'ta ekstra istek atılmaz. */
    private void addCrumbIfNeeded(RestClient.RequestBodySpec request) {
        if (!crumbEnabled) return;
        try {
            Map<String, Object> crumb = restClient.get()
                    .uri("/crumbIssuer/api/json").retrieve().body(MAP_TYPE);
            if (crumb == null) return;
            String field = str(crumb.get("crumbRequestField"));
            String value = str(crumb.get("crumb"));
            if (field != null && value != null) request.header(field, value);
        } catch (Exception e) {
            // Crumb alınamazsa istek yine denenir; gerçekten gerekliyse 403 ile anlaşılır hata döner
        }
    }

    // ─── URL yardımcıları ─────────────────────────────────────────────────────

    /** "scrumtools/deploy-test" → "/job/scrumtools/job/deploy-test" (segmentler encode edilir). */
    private String jobPath(String jobFullName) {
        StringBuilder path = new StringBuilder();
        for (String segment : jobFullName.split("/")) {
            if (segment.isBlank()) continue;
            path.append("/job/").append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        if (path.isEmpty()) throw new IllegalArgumentException("Geçersiz job adı: " + jobFullName);
        return path.toString();
    }

    /**
     * Jenkins'in döndürdüğü mutlak URL'i kendi baseUrl'imize çapalar.
     * marker (ör. "/job/") sonrası kısım alınır; bulunamazsa null döner.
     */
    private String reanchor(String absoluteUrl, String marker) {
        String tail = tailFrom(absoluteUrl, marker);
        return tail == null ? null : baseUrl + tail;
    }

    /** Çapalanmış URL'i RestClient'ın baseUrl'ine göre göreli yola çevirir. */
    private String relativize(String url, String marker) {
        String tail = tailFrom(url, marker);
        if (tail == null) {
            throw new CiApiException(502, PROVIDER + " adresi tanınamadı: " + url);
        }
        return tail.endsWith("/") ? tail : tail + "/";
    }

    private String tailFrom(String url, String marker) {
        if (url == null || url.isBlank()) return null;
        String path;
        try {
            URI uri = URI.create(url);
            path = uri.getPath() == null ? url : uri.getPath();
        } catch (IllegalArgumentException e) {
            path = url;
        }
        int index = path.indexOf(marker);
        return index < 0 ? null : path.substring(index);
    }

    // ─── Ortak küçük yardımcılar ──────────────────────────────────────────────

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static List<?> asList(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    private CiApiException wrap(RestClientResponseException e, String action) {
        int status = e.getStatusCode().value();
        String detail = switch (status) {
            case 401 -> "kullanıcı adı veya API token geçersiz";
            case 403 -> "kullanıcı yetkisiz ya da CSRF koruması engelledi (crumb ayarını kontrol edin)";
            case 404 -> "kaynak bulunamadı (job adı ve token'ın görme yetkisini kontrol edin)";
            case 409 -> "job şu anda tetiklenebilir durumda değil";
            default -> "HTTP " + status;
        };
        return new CiApiException(status, PROVIDER + " " + action + " başarısız: " + detail + ".", e);
    }

    private CiApiException unreachable(ResourceAccessException e, String action) {
        return new CiApiException(CiApiException.UNREACHABLE,
                PROVIDER + " " + action + " başarısız: sunucuya ulaşılamadı (adres/ağ erişimini kontrol edin).", e);
    }
}

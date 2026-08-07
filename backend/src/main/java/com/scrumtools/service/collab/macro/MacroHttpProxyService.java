package com.scrumtools.service.collab.macro;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.collab.CollabPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Makroların dış HTTP çağrılarını yapan vekil (COLLAB_WORKSPACE_PLAN.md §9.2).
 *
 * <p>Üç katman koruma:
 * <ol>
 *   <li><b>Allowlist</b> — {@code app.collab.macro.http-allowlist} boşsa özellik
 *       tamamen kapalıdır. Varsayılanın "kapalı" olması bilinçli: yanlışlıkla
 *       açık kalmış bir vekil, sunucuyu isteğe bağlı bir istek üreteci yapar.</li>
 *   <li><b>SSRF</b> — alan adı çözümlenir, özel/loopback/link-local adresler
 *       reddedilir. Yönlendirme takip edilmez; aksi hâlde allowlist'teki bir
 *       adres 302 ile iç ağa yönlendirebilirdi.</li>
 *   <li><b>Yetki</b> — çalıştıran kullanıcının {@code COLLAB_RUN_MACRO} izni ve
 *       organizasyonun {@code COLLAB_MACRO} paketi aranır.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MacroHttpProxyService {

    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE");

    /** İstemciden gelen çağrı. Başlıklar bilinçli olarak sınırlı tutuluyor. */
    public record ProxyRequest(String url, String method, Map<String, String> headers, String body) {
    }

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CollabPermissionService permissionService;
    private final EntitlementService entitlementService;

    @Value("${app.collab.macro.http-allowlist:}")
    private String allowlist;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            // Yönlendirme takip edilmiyor: allowlist yalnızca ilk adresi
            // doğrular, 302 zinciri o doğrulamayı anlamsız kılardı.
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public Map<String, Object> fetch(UUID projectId, ProxyRequest request) {
        User user = currentUser();
        if (!permissionService.hasProjectPermission(projectId, user, Permission.COLLAB_RUN_MACRO)) {
            throw new SecurityException("Makro çalıştırma yetkiniz yok.");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
        entitlementService.assertFeature(project.getOrganization(), PlanFeature.COLLAB_MACRO);

        Set<String> hosts = allowedHosts();
        if (hosts.isEmpty()) {
            throw new IllegalStateException(
                    "Makro HTTP erişimi kapalı. Yöneticiniz app.collab.macro.http-allowlist ayarını tanımlamalı.");
        }

        URI uri = URI.create(Objects.requireNonNull(request.url(), "url zorunlu"));
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Yalnızca https adresleri çağrılabilir.");
        }
        String host = uri.getHost();
        if (host == null || !hosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new SecurityException("Adres izin listesinde değil: " + host);
        }
        assertPublicAddress(host);

        String method = request.method() == null ? "GET" : request.method().toUpperCase(Locale.ROOT);
        if (!ALLOWED_METHODS.contains(method)) {
            throw new IllegalArgumentException("Desteklenmeyen metot: " + method);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .method(method, request.body() == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(request.body()));

        if (request.headers() != null) {
            request.headers().forEach((name, value) -> {
                // Hop-by-hop ve kimlik başlıkları geçirilmiyor: makro,
                // sunucunun kimliğini ödünç alamamalı.
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("proxy-") || lower.equals("host") || lower.equals("cookie")) return;
                builder.header(name, value);
            });
        }

        try {
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null && response.body().length() > MAX_RESPONSE_BYTES
                    ? response.body().substring(0, MAX_RESPONSE_BYTES)
                    : response.body();
            return Map.of(
                    "status", response.statusCode(),
                    "body", body != null ? body : "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("İstek yarıda kesildi.");
        } catch (Exception e) {
            log.warn("Makro HTTP vekili başarısız: {} — {}", uri, e.toString());
            throw new IllegalStateException("Dış istek başarısız: " + e.getMessage());
        }
    }

    private Set<String> allowedHosts() {
        if (allowlist == null || allowlist.isBlank()) return Set.of();
        Set<String> hosts = new HashSet<>();
        for (String entry : allowlist.split(",")) {
            String trimmed = entry.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) hosts.add(trimmed);
        }
        return hosts;
    }

    /** Allowlist'teki bir alan adı iç bir IP'ye çözümlenebilir — asıl SSRF kapısı budur. */
    private void assertPublicAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                        || address.isLinkLocalAddress() || address.isAnyLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new SecurityException("Özel ağ adresleri çağrılamaz: " + host);
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Adres çözümlenemedi: " + host);
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı: " + email));
    }
}

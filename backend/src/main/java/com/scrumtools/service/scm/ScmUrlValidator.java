package com.scrumtools.service.scm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Self-hosted baseUrl için SSRF koruması. Org admin'in girdiği URL'e
 * sunucudan istek atılacağı için private/loopback ağlara çözümlenen
 * hostlar varsayılan olarak reddedilir.
 *
 * On-prem kurulumlar için kaçış: SCM_ALLOW_PRIVATE_HOSTS=true.
 * Path'li URL'lere izin verilir (https://git.firma.com/gitlab gibi);
 * kullanıcı bilgisi (@) ve fragment reddedilir.
 */
@Component
public class ScmUrlValidator {

    private final boolean allowPrivateHosts;

    public ScmUrlValidator(@Value("${scm.allow-private-hosts:false}") boolean allowPrivateHosts) {
        this.allowPrivateHosts = allowPrivateHosts;
    }

    /** Geçersizse IllegalArgumentException fırlatır; null/boş URL geçerlidir (cloud varsayılanı). */
    public void validate(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return;

        URI uri;
        try {
            uri = URI.create(baseUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Geçersiz URL: " + baseUrl);
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Sadece http/https adresleri desteklenir.");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL içinde kullanıcı bilgisi (@) kullanılamaz.");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("URL fragment (#) içeremez.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL bir host içermeli.");
        }

        if (allowPrivateHosts) return;

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isPrivate(address)) {
                    throw new IllegalArgumentException(
                            "Bu adres iç ağa çözümleniyor ve güvenlik nedeniyle reddedildi: " + host);
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Host çözümlenemedi: " + host);
        }
    }

    private boolean isPrivate(InetAddress address) {
        // isSiteLocalAddress: 10/8, 172.16/12, 192.168/16
        return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || isUniqueLocalIpv6(address);
    }

    /** fc00::/7 — IPv6 unique local (isSiteLocalAddress bunu kapsamaz). */
    private boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }
}

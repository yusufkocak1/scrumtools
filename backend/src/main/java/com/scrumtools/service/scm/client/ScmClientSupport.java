package com.scrumtools.service.scm.client;

import com.scrumtools.service.scm.ScmDates;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** Client implementasyonlarının ortak yardımcıları. */
final class ScmClientSupport {

    private ScmClientSupport() {}

    static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> asList(Object o) {
        return o instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    /** ISO-8601 (offset'li) tarihi sistem zone'unda LocalDateTime'a çevirir. */
    static LocalDateTime parseDate(Object isoDate) {
        return ScmDates.parse(isoDate);
    }

    /** LocalDateTime'ı sağlayıcıların beklediği UTC ISO-8601 formatına çevirir. */
    static String toIsoUtc(LocalDateTime dateTime) {
        Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toString();
    }

    /** RestClient hatasını anlaşılır ScmApiException'a çevirir (token loglanmaz). */
    static ScmApiException wrap(RestClientResponseException e, String provider, String action) {
        int status = e.getStatusCode().value();
        String detail = switch (status) {
            case 401 -> "token geçersiz veya süresi dolmuş";
            case 403 -> "token yetkisiz ya da rate limit aşıldı";
            case 404 -> "kaynak bulunamadı (repo adı/izinleri kontrol edin)";
            case 422 -> "sağlayıcı isteği kabul etmedi (kayıt zaten mevcut olabilir)";
            default -> "HTTP " + status;
        };
        return new ScmApiException(status, provider + " " + action + " başarısız: " + detail + ".", e);
    }
}

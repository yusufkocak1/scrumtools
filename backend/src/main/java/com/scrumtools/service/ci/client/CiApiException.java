package com.scrumtools.service.ci.client;

/**
 * CI sağlayıcı API çağrısı başarısız olduğunda fırlatılır.
 * GlobalExceptionHandler'da RuntimeException olarak 400'e düşer;
 * statusCode ile kimlik hatası (401/403) ile ulaşılamama (0) ayrılabilir.
 */
public class CiApiException extends RuntimeException {

    /** Ağ hatası / timeout — HTTP yanıtı hiç alınamadı. */
    public static final int UNREACHABLE = 0;

    private final int statusCode;

    public CiApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public CiApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** Kimlik bilgileri geçersiz/yetkisiz mi — bağlantıyı INVALID işaretlemek için. */
    public boolean isAuthFailure() {
        return statusCode == 401 || statusCode == 403;
    }

    /** Sunucuya ulaşılamadı mı — poller'ın ardışık hata sayacı bunu sayar. */
    public boolean isUnreachable() {
        return statusCode == UNREACHABLE || statusCode >= 500;
    }
}

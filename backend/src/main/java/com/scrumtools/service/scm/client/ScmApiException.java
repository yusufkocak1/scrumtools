package com.scrumtools.service.scm.client;

/**
 * Sağlayıcı API çağrısı başarısız olduğunda fırlatılır.
 * GlobalExceptionHandler'da RuntimeException olarak 400'e düşer;
 * statusCode ile 401/403 (token) ve 429 (rate limit) ayrımı yapılabilir.
 */
public class ScmApiException extends RuntimeException {

    private final int statusCode;

    public ScmApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ScmApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** Token geçersiz/yetkisiz mi — bağlantıyı TOKEN_INVALID işaretlemek için. */
    public boolean isAuthFailure() {
        return statusCode == 401 || statusCode == 403;
    }
}

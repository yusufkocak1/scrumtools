package com.scrumtools.exception;

/**
 * Ödeme sağlayıcı yapılandırılmamışken (iyzico.enabled=false) online ödeme
 * başlatılmaya çalışılırsa fırlatılır. Manuel aktivasyon her zaman mümkündür.
 */
public class PaymentProviderUnavailableException extends RuntimeException {
    public PaymentProviderUnavailableException() {
        super("Online ödeme şu anda kullanılamıyor. Lütfen bizimle iletişime geçin; " +
                "ödemenizi havale/EFT ile alıp paketinizi hemen aktive edebiliriz.");
    }
}

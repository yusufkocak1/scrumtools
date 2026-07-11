package com.scrumtools.service.payment;

import com.scrumtools.exception.PaymentProviderUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * iyzico yapılandırılmamışken devreye giren fallback.
 * Online ödeme başlatılamaz; superadmin manuel aktivasyon akışı etkilenmez.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "iyzico.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPaymentProvider implements PaymentProviderService {

    @Override
    public PaymentLink createPaymentLink(CreateLinkCommand command) {
        log.warn("Ödeme linki istendi ama iyzico yapılandırılmamış (conversationId={})", command.conversationId());
        throw new PaymentProviderUnavailableException();
    }

    @Override
    public LinkStatus getLinkStatus(String token) {
        return new LinkStatus(false, null, null);
    }

    @Override
    public void disableLink(String token) {
        // no-op
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}

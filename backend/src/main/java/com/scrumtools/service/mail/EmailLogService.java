package com.scrumtools.service.mail;

import com.scrumtools.entity.EmailMessage;
import com.scrumtools.entity.EmailWebhookDelivery;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.EmailStatus;
import com.scrumtools.repository.EmailMessageRepository;
import com.scrumtools.repository.EmailWebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Gönderilen maillerin durum kaydı. Gönderim anında QUEUED satırı açılır,
 * PostForge webhook'ları {@link PostForgeWebhookService} üzerinden burayı günceller.
 * <p>
 * Şablon parametreleri saklanmaz — davet/sıfırlama linkleri tek kullanımlık token içerir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailLogService {

    private final EmailMessageRepository repository;
    private final EmailWebhookDeliveryRepository deliveryRepository;

    /**
     * Gönderim kaydını açar. Webhook, kayıt commit edilmeden önce gelebildiği için
     * upsert çalışır: satır varsa yalnızca eksik alanlar (org/user) tamamlanır,
     * webhook'un yazdığı durum ezilmez.
     * <p>
     * REQUIRES_NEW: mail gönderimi çağıran iş akışının (üye oluşturma) transaction'ına
     * bağlı olmamalı — kayıt hatası üye oluşturmayı geri almamalı.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordQueued(String messageId, String templateCode, String recipient,
                             Organization organization, User user) {
        if (messageId == null || messageId.isBlank()) {
            // messageId yoksa webhook ile eşleşemez; kayıt tutmanın anlamı kalmaz
            log.warn("E-posta kaydı atlandı ({} → {}): PostForge messageId dönmedi",
                    templateCode, recipient);
            return;
        }
        try {
            EmailMessage existing = repository.findByMessageId(messageId).orElse(null);
            if (existing != null) {
                if (existing.getOrganization() == null) existing.setOrganization(organization);
                if (existing.getUser() == null) existing.setUser(user);
                repository.save(existing);
                return;
            }
            repository.save(EmailMessage.builder()
                    .messageId(messageId)
                    .templateCode(templateCode)
                    .recipient(recipient)
                    .organization(organization)
                    .user(user)
                    .status(EmailStatus.QUEUED)
                    .build());
        } catch (Exception e) {
            // Kayıt tutulamaması mail gönderimini bozmamalı
            log.error("E-posta kaydı yazılamadı ({} → {}): {}", templateCode, recipient, e.getMessage());
        }
    }

    /**
     * Webhook olayını uygular. Yalnızca davet mailleri takip edildiği için diğer
     * şablonların olayları yok sayılır.
     * <p>
     * Bilinmeyen messageId'li DAVET olayında satır açılır: webhook, gönderim kaydı
     * commit edilmeden önce gelebilir (nadir) ve olay aksi halde kaybolurdu.
     * <p>
     * Tekrar kaydı ile olayın uygulanması TEK transaction'dadır: işleme hata verirse
     * kayıt da geri alınır, böylece olay "işlendi" işaretli kalıp kaybolmaz. Tekrar
     * denendiğinde unique kısıt yarışı da aynı şekilde rollback'e düşer — sayaçlar
     * hiçbir durumda iki kez artmaz.
     */
    @Transactional
    public void applyEvent(String event, String messageId, String recipient, String templateCode,
                           String failureReason, LocalDateTime occurredAt, String deliveryId) {
        if (isAlreadyProcessed(deliveryId)) {
            log.debug("PostForge webhook: tekrar eden olay atlandı (delivery={})", deliveryId);
            return;
        }

        EmailMessage message = repository.findByMessageId(messageId).orElse(null);
        if (message == null) {
            if (!PostForgeMailService.T_MEMBER_INVITE.equals(templateCode)) {
                log.debug("PostForge webhook: takip edilmeyen şablon, atlandı ({})", templateCode);
                return;
            }
            message = EmailMessage.builder()
                    .messageId(messageId)
                    .templateCode(templateCode)
                    .recipient(recipient != null ? recipient : "-")
                    .status(EmailStatus.QUEUED)
                    .build();
        }

        switch (event) {
            case "message.sent" -> {
                message.setStatus(EmailStatus.SENT);
                message.setSentAt(occurredAt);
            }
            case "message.failed" -> {
                message.setStatus(EmailStatus.FAILED);
                message.setFailedAt(occurredAt);
                message.setFailureReason(truncate(failureReason, 500));
            }
            case "message.opened" -> {
                if (message.getOpenedAt() == null) message.setOpenedAt(occurredAt);
                message.setOpenCount(message.getOpenCount() + 1);
            }
            case "message.clicked" -> {
                if (message.getFirstClickedAt() == null) message.setFirstClickedAt(occurredAt);
                message.setClickCount(message.getClickCount() + 1);
                // Tıklanan mail açılmış da sayılır (açılma pikseli engellenmiş olabilir)
                if (message.getOpenedAt() == null) message.setOpenedAt(occurredAt);
            }
            default -> {
                log.debug("PostForge webhook: bilinmeyen olay türü, atlandı: {}", event);
                return;
            }
        }

        message.setLastEventAt(occurredAt);
        repository.save(message);
        log.debug("E-posta durumu güncellendi: {} → {}", messageId, event);
    }

    /**
     * Olay daha önce işlendiyse true; işlenmediyse kimliği kaydedip false döner.
     * Kimlik yoksa tekrar kontrolü yapılamaz — olay yine de işlenir (kayıp bildirim,
     * şişmiş sayaçtan daha kötüdür).
     */
    private boolean isAlreadyProcessed(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) return false;
        if (deliveryRepository.existsByDeliveryId(deliveryId)) return true;
        deliveryRepository.save(EmailWebhookDelivery.builder().deliveryId(deliveryId).build());
        return false;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}

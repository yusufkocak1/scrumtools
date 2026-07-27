package com.scrumtools.service.mail;

import com.scrumtools.entity.Organization;
import com.scrumtools.entity.User;

import java.time.LocalDateTime;

/**
 * Uygulama e-postaları. Organizasyon maillerinin alıcısı org sahibidir.
 * Implementasyonlar hata FIRLATMAZ (loglar) — mail gönderilemedi diye
 * iş akışı (üye oluşturma, ödeme aktivasyonu vb.) geri alınmamalıdır.
 */
public interface MailService {

    /**
     * Org sahibinin oluşturduğu yeni üyeye şifre-kurulum linki.
     * Organizasyon, davetin durum kaydını (bkz. EmailLogService) ilgili org'a
     * bağlamak için gerekir — davet listesi bu bağa göre filtrelenir.
     */
    void sendMemberInvite(User user, Organization org, String setupUrl);

    /** Şifremi unuttum akışı. */
    void sendPasswordReset(User user, String resetUrl);

    /** Trial bitişine X gün kala hatırlatma (org sahibine). */
    void sendTrialExpiring(Organization org, long daysLeft);

    /** Trial süresi doldu, FREE'ye düşüldü. */
    void sendTrialExpired(Organization org);

    /** Superadmin veya sistemin oluşturduğu ödeme linki. */
    void sendPaymentLink(Organization org, String planName, String cycleLabel, String paymentUrl);

    /** Ödeme alındı, abonelik aktive edildi. */
    void sendPaymentReceived(Organization org, String planName, LocalDateTime periodEnd);

    /** Abonelik bitişine X gün kala yenileme hatırlatması (varsa ödeme linkiyle). */
    void sendSubscriptionExpiring(Organization org, long daysLeft, String renewUrl);

    /** Abonelik süresi doldu, FREE'ye düşüldü. */
    void sendSubscriptionExpired(Organization org);
}

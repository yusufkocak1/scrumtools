package com.scrumtools.dto;

import com.scrumtools.entity.enums.EmailStatus;
import com.scrumtools.entity.enums.InvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organizasyonun GÖNDERDİĞİ bir davet. İki akış tek listede birleşir:
 * <ul>
 *   <li>Hesabı olmayan davetli → şifre-kurulum maili gider, mail alanları dolar.</li>
 *   <li>Hesabı olan davetli → uygulama içi davet oluşur, mail gitmez ({@code mailStatus} null).</li>
 * </ul>
 */
public record InviteResponse(
        UUID id,
        String email,
        /** Davetlinin hesabı varsa adı; yoksa null. */
        String name,
        /** PENDING/ACCEPTED/DECLINED/EXPIRED — süresi geçmiş PENDING'ler EXPIRED döner. */
        InvitationStatus inviteStatus,
        String invitedByName,
        LocalDateTime invitedAt,
        LocalDateTime expiresAt,

        /** Mail gönderilmediyse (uygulama içi davet) null. */
        EmailStatus mailStatus,
        LocalDateTime mailSentAt,
        LocalDateTime openedAt,
        LocalDateTime firstClickedAt,
        int clickCount,
        String failureReason,

        /** Davetli şifresini belirleyip hesabı etkinleştirdi mi. */
        boolean accountActivated
) {}

package com.scrumtools.dto;

import com.scrumtools.entity.enums.EmailStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organizasyona gönderilmiş bir davet ve mailin durumu.
 * Durum alanları PostForge webhook'larıyla güncellenir.
 */
public record InviteResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        EmailStatus status,
        LocalDateTime sentAt,
        LocalDateTime openedAt,
        LocalDateTime firstClickedAt,
        int clickCount,
        String failureReason,
        /** Davetli şifresini belirleyip hesabı etkinleştirdi mi. */
        boolean accountActivated,
        LocalDateTime invitedAt
) {}

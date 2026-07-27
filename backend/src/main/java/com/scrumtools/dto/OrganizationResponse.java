package com.scrumtools.dto;

import com.scrumtools.entity.enums.OrgRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String logoUrl,
        UUID ownerId,
        String ownerName,
        String plan,
        Integer maxMembers,
        int memberCount,
        LocalDateTime createdAt,
        /**
         * İsteği yapan kullanıcının bu organizasyondaki rolü. Arayüz menü ve
         * aksiyonları buna göre gizler — kullanıcı yalnızca tıklayınca "yetkiniz
         * yok" uyarısı almamalı.
         */
        OrgRole myRole
) {}


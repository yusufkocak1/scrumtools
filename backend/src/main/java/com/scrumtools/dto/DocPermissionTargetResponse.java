package com.scrumtools.dto;

import com.scrumtools.entity.enums.DocTargetType;

import java.util.UUID;

/**
 * Yetki hedefi arama sonucu: kullanıcı, takım, organizasyon veya proje.
 */
public record DocPermissionTargetResponse(
        UUID id,
        String name,
        String detail,
        DocTargetType targetType
) {}

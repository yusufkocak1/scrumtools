package com.scrumtools.dto;

import com.scrumtools.entity.enums.DocAccessLevel;
import com.scrumtools.entity.enums.DocTargetType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DocPermissionRequest(
        UUID spaceId,
        UUID pageId,

        @NotNull(message = "Erişim seviyesi belirtilmeli")
        DocAccessLevel accessLevel,

        @NotNull(message = "Hedef tipi belirtilmeli")
        DocTargetType targetType,

        @NotNull(message = "Hedef ID belirtilmeli")
        UUID targetId,

        boolean canDelegate
) {}


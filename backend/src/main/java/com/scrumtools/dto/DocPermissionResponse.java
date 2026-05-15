package com.scrumtools.dto;

import com.scrumtools.entity.DocPermission;
import com.scrumtools.entity.enums.DocAccessLevel;
import com.scrumtools.entity.enums.DocTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocPermissionResponse(
        UUID id,
        UUID spaceId,
        UUID pageId,
        DocAccessLevel accessLevel,
        DocTargetType targetType,
        UUID targetId,
        String targetName,
        String grantedByEmail,
        String grantedByName,
        boolean canDelegate,
        LocalDateTime createdAt
) {
    public static DocPermissionResponse from(DocPermission p, String targetName) {
        return new DocPermissionResponse(
                p.getId(),
                p.getSpace() != null ? p.getSpace().getId() : null,
                p.getPage() != null ? p.getPage().getId() : null,
                p.getAccessLevel(),
                p.getTargetType(),
                p.getTargetId(),
                targetName,
                p.getGrantedBy().getEmail(),
                p.getGrantedBy().getName(),
                p.getCanDelegate(),
                p.getCreatedAt()
        );
    }
}


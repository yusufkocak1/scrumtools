package com.scrumtools.dto;

import com.scrumtools.entity.TaskLink;
import com.scrumtools.entity.enums.LinkType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskLinkResponse(
        UUID id,
        String sourceTaskId,
        String sourceTaskCustomId,
        String sourceTaskTitle,
        // İlişkili işin durumu — detay sayfası bağlantı listesinde rozet olarak gösterir
        String sourceTaskStatus,
        String targetTaskId,
        String targetTaskCustomId,
        String targetTaskTitle,
        String targetTaskStatus,
        LinkType linkType,
        String linkTypeLabel,
        String createdBy,
        LocalDateTime createdAt
) {
    public static TaskLinkResponse from(TaskLink link) {
        return new TaskLinkResponse(
                link.getId(),
                link.getSourceTask().getId().toString(),
                link.getSourceTask().getCustomId(),
                link.getSourceTask().getTitle(),
                link.getSourceTask().getStatus(),
                link.getTargetTask().getId().toString(),
                link.getTargetTask().getCustomId(),
                link.getTargetTask().getTitle(),
                link.getTargetTask().getStatus(),
                link.getLinkType(),
                link.getLinkType().getLabel(),
                link.getCreatedBy(),
                link.getCreatedAt()
        );
    }
}


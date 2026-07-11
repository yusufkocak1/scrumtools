package com.scrumtools.dto;

import com.scrumtools.entity.Task;

import java.util.UUID;

/** Takım içi task typeahead araması sonucu (autocomplete picker için). */
public record TaskSearchResponse(
        UUID id,
        String customId,
        String title,
        String status,
        String issueType,
        boolean hasParent
) {
    public static TaskSearchResponse from(Task task) {
        return new TaskSearchResponse(
                task.getId(),
                task.getCustomId(),
                task.getTitle(),
                task.getStatus(),
                task.getIssueType(),
                task.getParentTask() != null
        );
    }
}

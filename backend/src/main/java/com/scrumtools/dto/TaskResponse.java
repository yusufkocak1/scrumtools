package com.scrumtools.dto;

import com.scrumtools.entity.Task;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class TaskResponse {

    private String id;
    private String teamId;
    private String sprintId;
    private String parentTaskId;
    private String customId;
    private String title;
    private String description;
    private String status;
    private String issueType;
    private String priority;
    private String reporter;
    private String assignee;
    private String developer;
    private String analyst;
    private String tester;
    private Integer storyPoints;
    private List<String> labels;
    private List<String> watchers;

    // Faz 3 alanları
    private LocalDate dueDate;
    private LocalDate startDate;
    private Double estimatedHours;
    private Double loggedHours;
    private String environment;
    private String resolution;
    private LocalDateTime resolvedAt;
    private Integer position;
    private Integer subtaskCount;

    private Map<String, Object> customFields;
    private List<CommentDto> comments;
    private List<AttachmentDto> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class CommentDto {
        private String id;
        private String author;
        private String text;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class AttachmentDto {
        private String id;
        private String fileName;
        private Long fileSize;
        private String mimeType;
        private String uploadedBy;
        private LocalDateTime createdAt;
    }

    public static TaskResponse from(Task task) {
        List<CommentDto> comments = task.getComments() == null ? List.of() :
                task.getComments().stream()
                        .map(c -> CommentDto.builder()
                                .id(c.getId().toString())
                                .author(c.getAuthor())
                                .text(c.getText())
                                .createdAt(c.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        List<AttachmentDto> attachments = task.getAttachments() == null ? List.of() :
                task.getAttachments().stream()
                        .map(a -> AttachmentDto.builder()
                                .id(a.getId().toString())
                                .fileName(a.getFileName())
                                .fileSize(a.getFileSize())
                                .mimeType(a.getMimeType())
                                .uploadedBy(a.getUploadedBy())
                                .createdAt(a.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        return TaskResponse.builder()
                .id(task.getId().toString())
                .teamId(task.getTeam().getId().toString())
                .sprintId(task.getSprint() != null ? task.getSprint().getId().toString() : null)
                .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId().toString() : null)
                .customId(task.getCustomId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .issueType(task.getIssueType())
                .priority(task.getPriority())
                .reporter(task.getReporter())
                .assignee(task.getAssignee())
                .developer(task.getDeveloper())
                .analyst(task.getAnalyst())
                .tester(task.getTester())
                .storyPoints(task.getStoryPoints())
                .labels(task.getLabels())
                .watchers(task.getWatchers())
                .dueDate(task.getDueDate())
                .startDate(task.getStartDate())
                .estimatedHours(task.getEstimatedHours())
                .loggedHours(task.getLoggedHours())
                .environment(task.getEnvironment())
                .resolution(task.getResolution())
                .resolvedAt(task.getResolvedAt())
                .position(task.getPosition())
                .subtaskCount(task.getSubtasks() != null ? task.getSubtasks().size() : 0)
                .customFields(task.getCustomFields())
                .comments(comments)
                .attachments(attachments)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

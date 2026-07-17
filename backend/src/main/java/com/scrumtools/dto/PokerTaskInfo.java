package com.scrumtools.dto;

import com.scrumtools.entity.Task;

/**
 * Poker oturumuna bağlanan görevin özet bilgisi.
 * Hem PokerSessionResponse içinde hem de /topic/poker/{teamId}/task broadcast'inde kullanılır.
 */
public record PokerTaskInfo(
        String taskId,
        String customId,
        String title,
        String issueType,
        Integer storyPoints
) {
    public static PokerTaskInfo from(Task task) {
        return new PokerTaskInfo(
                task.getId().toString(),
                task.getCustomId(),
                task.getTitle(),
                task.getIssueType(),
                task.getStoryPoints()
        );
    }
}

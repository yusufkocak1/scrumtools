package com.scrumtools.dto;

/**
 * Leaderboard sıralamasındaki bir giriş.
 */
public record QuizLeaderboardEntry(
        int rank,
        String email,
        String displayName,
        int totalScore,
        int correctCount,
        int answeredCount
) {
}


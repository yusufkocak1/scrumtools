package com.scrumtools.dto;

/**
 * Puan sıralamasındaki bir giriş. İzleyiciler (moderatör sunucu modundayken) bu listede yer almaz.
 */
public record HangmanLeaderboardEntry(
        int rank,
        String email,
        String displayName,
        int totalScore,
        int wordsSolved,
        int correctLetterCount,
        int wrongLetterCount
) {
}

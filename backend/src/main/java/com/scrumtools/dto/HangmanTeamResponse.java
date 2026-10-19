package com.scrumtools.dto;

/**
 * Takım modunda bir takımın özeti. Üyeler katılımcı listesindeki {@code teamNo} ile bulunur.
 */
public record HangmanTeamResponse(
        int teamNo,
        String name,
        int score,
        int memberCount
) {
}

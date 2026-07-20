package com.scrumtools.dto;

import com.scrumtools.entity.HangmanGuess;

import java.time.LocalDateTime;

/**
 * Canlı tahmin akışı girişi ("Ayşe 'K' dedi → +10").
 */
public record HangmanGuessResponse(
        String userEmail,
        String displayName,
        String guessType,
        String value,
        boolean correct,
        int scoreDelta,
        LocalDateTime createdAt
) {
    public static HangmanGuessResponse from(HangmanGuess g) {
        return new HangmanGuessResponse(
                g.getUserEmail(),
                g.getDisplayName(),
                g.getGuessType().name(),
                g.getValue(),
                Boolean.TRUE.equals(g.getCorrect()),
                g.getScoreDelta(),
                g.getCreatedAt()
        );
    }
}

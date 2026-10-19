package com.scrumtools.dto;

import com.scrumtools.entity.HangmanGuess;

import java.time.LocalDateTime;
import java.util.List;

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
        /** Bu harfi önceden doğru tahmin edip bonus alanlar; kimse yoksa boş. */
        List<String> predictionWinners,
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
                g.getPredictionWinners() == null || g.getPredictionWinners().isBlank()
                        ? List.of()
                        : List.of(g.getPredictionWinners().split("\n")),
                g.getCreatedAt()
        );
    }
}

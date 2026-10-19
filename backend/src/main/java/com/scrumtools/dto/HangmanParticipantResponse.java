package com.scrumtools.dto;

import com.scrumtools.entity.HangmanParticipant;

public record HangmanParticipantResponse(
        String email,
        String displayName,
        int turnOrder,
        boolean spectator,
        int totalScore,
        int correctLetterCount,
        int wrongLetterCount,
        int wordsSolved,
        int predictionHits,
        int wrongWordCount,
        /** Takım modunda 1 | 2; diğer modlarda null. */
        Integer teamNo
) {
    public static HangmanParticipantResponse from(HangmanParticipant p) {
        return new HangmanParticipantResponse(
                p.getUserEmail(),
                p.getDisplayName(),
                p.getTurnOrder(),
                Boolean.TRUE.equals(p.getSpectator()),
                p.getTotalScore(),
                p.getCorrectLetterCount(),
                p.getWrongLetterCount(),
                p.getWordsSolved(),
                p.getPredictionHits() == null ? 0 : p.getPredictionHits(),
                p.getWrongWordCount() == null ? 0 : p.getWrongWordCount(),
                p.getTeamNo()
        );
    }
}

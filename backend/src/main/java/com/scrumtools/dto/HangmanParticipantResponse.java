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
        int wordsSolved
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
                p.getWordsSolved()
        );
    }
}

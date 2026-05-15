package com.scrumtools.dto;

import com.scrumtools.entity.QuizParticipant;

public record QuizParticipantResponse(
        String email,
        String displayName,
        int totalScore,
        int correctCount,
        int answeredCount
) {
    public static QuizParticipantResponse from(QuizParticipant p) {
        return new QuizParticipantResponse(
                p.getUserEmail(),
                p.getDisplayName(),
                p.getTotalScore(),
                p.getCorrectCount(),
                p.getAnsweredCount()
        );
    }
}


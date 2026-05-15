package com.scrumtools.dto;

import com.scrumtools.entity.QuizQuestion;

import java.util.List;

public record QuizQuestionResponse(
        String id,
        int questionOrder,
        String questionText,
        List<String> options,
        int correctOptionIndex,
        int timeLimitSeconds
) {
    public static QuizQuestionResponse from(QuizQuestion q) {
        return new QuizQuestionResponse(
                q.getId().toString(),
                q.getQuestionOrder(),
                q.getQuestionText(),
                q.getOptions(),
                q.getCorrectOptionIndex(),
                q.getTimeLimitSeconds()
        );
    }

    /**
     * Oyunculara gönderilirken doğru cevap gizlenir.
     */
    public static QuizQuestionResponse forPlayer(QuizQuestion q) {
        return new QuizQuestionResponse(
                q.getId().toString(),
                q.getQuestionOrder(),
                q.getQuestionText(),
                q.getOptions(),
                -1, // doğru cevap gizli
                q.getTimeLimitSeconds()
        );
    }
}


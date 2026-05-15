package com.scrumtools.dto;

import com.scrumtools.entity.QuizAnswer;

public record QuizAnswerResponse(
        String questionId,
        String userEmail,
        int selectedOptionIndex,
        long answeredInMs,
        int score,
        boolean correct,
        int correctOptionIndex
) {
    /**
     * Cevap kaydedildiğinde — doğru cevap ve puan gizli.
     */
    public static QuizAnswerResponse hidden(QuizAnswer a) {
        return new QuizAnswerResponse(
                a.getQuestion().getId().toString(),
                a.getUserEmail(),
                a.getSelectedOptionIndex(),
                a.getAnsweredInMs() != null ? a.getAnsweredInMs() : 0,
                -1,    // puan gizli
                false, // doğruluk gizli
                -1     // doğru cevap gizli
        );
    }

    /**
     * Sonuç açıklandığında (rapor/reveal) — tüm bilgiler açık.
     */
    public static QuizAnswerResponse from(QuizAnswer a) {
        return new QuizAnswerResponse(
                a.getQuestion().getId().toString(),
                a.getUserEmail(),
                a.getSelectedOptionIndex(),
                a.getAnsweredInMs() != null ? a.getAnsweredInMs() : 0,
                a.getScore(),
                a.getCorrect(),
                a.getQuestion().getCorrectOptionIndex()
        );
    }
}


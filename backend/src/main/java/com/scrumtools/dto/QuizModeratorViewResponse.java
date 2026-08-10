package com.scrumtools.dto;

import java.util.List;

/**
 * Moderatör paneli verisi — yalnızca oturumun moderatörüne döner.
 *
 * Oyunculara giden WebSocket state'i doğru cevabı gizler; moderatör ise soruyu,
 * doğru cevabı ve kimin ne cevapladığını canlı görür. Bu yüzden ayrı bir uç nokta:
 * ortak topic'e moderatöre özel bilgi konulamaz.
 */
public record QuizModeratorViewResponse(
        String sessionId,
        String status,
        int currentQuestionIndex,
        int totalQuestions,
        QuizQuestionResponse currentQuestion,
        Long questionStartedAtMs,
        int answeredCount,
        int totalParticipants,
        List<Integer> optionCounts,
        List<ParticipantAnswerStatus> answerStatuses,
        List<QuizQuestionResponse> questions
) {
    /** Aktif sorudaki katılımcı durumu. */
    public record ParticipantAnswerStatus(
            String email,
            String displayName,
            boolean answered,
            int selectedOptionIndex,
            boolean correct,
            int score,
            long answeredInMs,
            int totalScore
    ) {
    }
}

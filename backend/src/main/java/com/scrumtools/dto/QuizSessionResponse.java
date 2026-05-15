package com.scrumtools.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scrumtools.entity.QuizSession;

import java.time.LocalDateTime;
import java.util.List;

public record QuizSessionResponse(
        String id,
        String teamId,
        String templateId,
        String templateTitle,
        String hostEmail,
        String hostName,
        String status,
        int currentQuestionIndex,
        int totalQuestions,
        QuizQuestionResponse currentQuestion,
        Long questionStartedAtMs,
        List<QuizParticipantResponse> participants,
        List<QuizLeaderboardEntry> leaderboard,
        @JsonProperty("resultsRevealed") boolean resultsRevealed,
        int correctOptionIndex,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
    public static QuizSessionResponse from(QuizSession s,
                                           QuizQuestionResponse currentQ,
                                           List<QuizParticipantResponse> participants,
                                           List<QuizLeaderboardEntry> leaderboard,
                                           boolean resultsRevealed,
                                           int correctOptionIndex) {
        return new QuizSessionResponse(
                s.getId().toString(),
                s.getTeam().getId().toString(),
                s.getTemplate().getId().toString(),
                s.getTemplate().getTitle(),
                s.getHostEmail(),
                s.getHostName(),
                s.getStatus().name(),
                s.getCurrentQuestionIndex(),
                s.getTemplate().getQuestions().size(),
                currentQ,
                s.getQuestionStartedAtMs(),
                participants,
                leaderboard,
                resultsRevealed,
                correctOptionIndex,
                s.getCreatedAt(),
                s.getStartedAt(),
                s.getFinishedAt()
        );
    }
}


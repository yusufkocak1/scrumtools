package com.scrumtools.dto;

import com.scrumtools.entity.QuizTemplate;

import java.time.LocalDateTime;
import java.util.List;

public record QuizTemplateResponse(
        String id,
        String teamId,
        String title,
        String description,
        String createdByEmail,
        String createdByName,
        int questionCount,
        List<QuizQuestionResponse> questions,
        LocalDateTime createdAt
) {
    public static QuizTemplateResponse from(QuizTemplate t) {
        List<QuizQuestionResponse> questions = t.getQuestions().stream()
                .map(QuizQuestionResponse::from)
                .toList();
        return new QuizTemplateResponse(
                t.getId().toString(),
                t.getTeam().getId().toString(),
                t.getTitle(),
                t.getDescription(),
                t.getCreatedByEmail(),
                t.getCreatedByName(),
                questions.size(),
                questions,
                t.getCreatedAt()
        );
    }

    public static QuizTemplateResponse summary(QuizTemplate t) {
        return new QuizTemplateResponse(
                t.getId().toString(),
                t.getTeam().getId().toString(),
                t.getTitle(),
                t.getDescription(),
                t.getCreatedByEmail(),
                t.getCreatedByName(),
                t.getQuestions().size(),
                null,
                t.getCreatedAt()
        );
    }
}


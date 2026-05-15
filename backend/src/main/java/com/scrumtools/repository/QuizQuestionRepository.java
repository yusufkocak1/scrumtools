package com.scrumtools.repository;

import com.scrumtools.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {
    List<QuizQuestion> findByTemplateIdOrderByQuestionOrderAsc(UUID templateId);
}


package com.scrumtools.repository;

import com.scrumtools.entity.QuizTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizTemplateRepository extends JpaRepository<QuizTemplate, UUID> {
    List<QuizTemplate> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
}


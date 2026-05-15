package com.scrumtools.repository;

import com.scrumtools.entity.QuizSession;
import com.scrumtools.entity.QuizSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizSessionRepository extends JpaRepository<QuizSession, UUID> {
    List<QuizSession> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    Optional<QuizSession> findByTeamIdAndStatus(UUID teamId, QuizSessionStatus status);

    List<QuizSession> findByTeamIdAndStatusOrderByCreatedAtDesc(UUID teamId, QuizSessionStatus status);
}


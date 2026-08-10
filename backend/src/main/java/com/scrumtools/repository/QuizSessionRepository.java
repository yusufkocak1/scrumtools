package com.scrumtools.repository;

import com.scrumtools.entity.QuizSession;
import com.scrumtools.entity.QuizSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuizSessionRepository extends JpaRepository<QuizSession, UUID> {
    List<QuizSession> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    List<QuizSession> findByTeamIdAndStatusOrderByCreatedAtDesc(UUID teamId, QuizSessionStatus status);

    /** Aynı takımda birden fazla yarışma aynı anda sürebilir — hepsi listelenir. */
    List<QuizSession> findByTeamIdAndStatusInOrderByCreatedAtDesc(UUID teamId,
                                                                  Collection<QuizSessionStatus> statuses);
}


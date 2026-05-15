package com.scrumtools.repository;

import com.scrumtools.entity.QuizParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizParticipantRepository extends JpaRepository<QuizParticipant, UUID> {
    List<QuizParticipant> findBySessionIdOrderByTotalScoreDesc(UUID sessionId);

    Optional<QuizParticipant> findBySessionIdAndUserEmail(UUID sessionId, String userEmail);

    boolean existsBySessionIdAndUserEmail(UUID sessionId, String userEmail);
}


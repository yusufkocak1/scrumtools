package com.scrumtools.repository;

import com.scrumtools.entity.HangmanParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HangmanParticipantRepository extends JpaRepository<HangmanParticipant, UUID> {

    List<HangmanParticipant> findBySessionIdOrderByTotalScoreDesc(UUID sessionId);

    /** Tur sırası bu listeye göre döner. */
    List<HangmanParticipant> findBySessionIdOrderByTurnOrderAsc(UUID sessionId);

    Optional<HangmanParticipant> findBySessionIdAndUserEmail(UUID sessionId, String userEmail);

    boolean existsBySessionIdAndUserEmail(UUID sessionId, String userEmail);
}

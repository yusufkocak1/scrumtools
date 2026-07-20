package com.scrumtools.repository;

import com.scrumtools.entity.HangmanRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HangmanRoundRepository extends JpaRepository<HangmanRound, UUID> {

    List<HangmanRound> findBySessionIdOrderByRoundOrderAsc(UUID sessionId);

    Optional<HangmanRound> findBySessionIdAndRoundOrder(UUID sessionId, Integer roundOrder);
}

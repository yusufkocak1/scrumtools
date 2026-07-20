package com.scrumtools.repository;

import com.scrumtools.entity.HangmanGuess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HangmanGuessRepository extends JpaRepository<HangmanGuess, UUID> {

    List<HangmanGuess> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    List<HangmanGuess> findByRoundIdOrderByCreatedAtAsc(UUID roundId);
}

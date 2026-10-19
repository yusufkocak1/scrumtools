package com.scrumtools.repository;

import com.scrumtools.entity.HangmanRaceBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HangmanRaceBoardRepository extends JpaRepository<HangmanRaceBoard, UUID> {

    List<HangmanRaceBoard> findByRoundId(UUID roundId);

    Optional<HangmanRaceBoard> findByRoundIdAndUserEmail(UUID roundId, String userEmail);
}

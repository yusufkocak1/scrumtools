package com.scrumtools.repository;

import com.scrumtools.entity.HangmanSession;
import com.scrumtools.entity.HangmanSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HangmanSessionRepository extends JpaRepository<HangmanSession, UUID> {

    Optional<HangmanSession> findByTeamIdAndStatus(UUID teamId, HangmanSessionStatus status);

    List<HangmanSession> findByTeamIdAndStatusOrderByCreatedAtDesc(UUID teamId, HangmanSessionStatus status);
}

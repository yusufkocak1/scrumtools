package com.scrumtools.repository;

import com.scrumtools.entity.PokerSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PokerSessionRepository extends JpaRepository<PokerSession, UUID> {
    Optional<PokerSession> findByTeamId(UUID teamId);
}


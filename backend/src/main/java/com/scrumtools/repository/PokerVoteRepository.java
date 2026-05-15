package com.scrumtools.repository;

import com.scrumtools.entity.PokerVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokerVoteRepository extends JpaRepository<PokerVote, UUID> {
    List<PokerVote> findByTeamId(UUID teamId);
    Optional<PokerVote> findByTeamIdAndUserEmail(UUID teamId, String userEmail);
    void deleteByTeamIdAndUserEmail(UUID teamId, String userEmail);
}


package com.scrumtools.repository;

import com.scrumtools.entity.RetroItemVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RetroItemVoteRepository extends JpaRepository<RetroItemVote, UUID> {
    Optional<RetroItemVote> findByRetroItemIdAndOwner(UUID retroItemId, String owner);
}


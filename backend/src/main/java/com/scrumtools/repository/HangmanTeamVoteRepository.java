package com.scrumtools.repository;

import com.scrumtools.entity.HangmanTeamVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HangmanTeamVoteRepository extends JpaRepository<HangmanTeamVote, UUID> {

    List<HangmanTeamVote> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    Optional<HangmanTeamVote> findBySessionIdAndUserEmail(UUID sessionId, String userEmail);

    /** Türetilmiş silme: kayıtları yükleyip tek tek siler, oturumdaki önbellekle tutarlı kalır. */
    void deleteBySessionId(UUID sessionId);
}

package com.scrumtools.repository;

import com.scrumtools.entity.HangmanPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HangmanPredictionRepository extends JpaRepository<HangmanPrediction, UUID> {

    List<HangmanPrediction> findBySessionId(UUID sessionId);

    Optional<HangmanPrediction> findBySessionIdAndUserEmail(UUID sessionId, String userEmail);

    long countBySessionId(UUID sessionId);

    /** Türetilmiş silme: kayıtları yükleyip tek tek siler, oturumdaki önbellekle tutarlı kalır. */
    void deleteBySessionId(UUID sessionId);
}

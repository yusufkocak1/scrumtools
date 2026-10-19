package com.scrumtools.repository;

import com.scrumtools.entity.HangmanSession;
import com.scrumtools.entity.HangmanSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HangmanSessionRepository extends JpaRepository<HangmanSession, UUID> {

    Optional<HangmanSession> findByTeamIdAndStatus(UUID teamId, HangmanSessionStatus status);

    List<HangmanSession> findByTeamIdAndStatusOrderByCreatedAtDesc(UUID teamId, HangmanSessionStatus status);

    /** Süre dolmuş sıraları tarayan zamanlayıcı için — aktif oyunlar. */
    List<HangmanSession> findByStatus(HangmanSessionStatus status);

    /**
     * Tur arasından çıkışı atomik olarak sahiplenir: moderatörün tıklaması ile otomatik
     * geçiş aynı anda gelirse yalnızca biri 1 döner, diğeri 0 — böylece tur iki kez atlanmaz.
     */
    @Modifying
    @Query("update HangmanSession s set s.awaitingNextRound = false "
            + "where s.id = :id and s.awaitingNextRound = true")
    int claimIntermissionExit(@Param("id") UUID id);
}

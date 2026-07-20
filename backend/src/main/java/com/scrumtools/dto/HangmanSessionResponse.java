package com.scrumtools.dto;

import com.scrumtools.entity.HangmanSession;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Oturumun istemciye giden tam durumu. WebSocket ile de aynı gövde yayınlanır.
 */
public record HangmanSessionResponse(
        String id,
        String teamId,
        String hostEmail,
        String hostName,
        String status,
        String language,
        String wordSource,
        boolean moderatorPlays,
        int currentRoundIndex,
        int totalRounds,
        String currentTurnEmail,
        String currentTurnName,
        /** Aktif tur — lobide ve oyun bitince null olabilir. */
        HangmanRoundResponse round,
        /**
         * En son biten tur (cevabı açık). Tur bitince sunucu hemen sonrakine geçtiği için
         * istemci "kelime neydi" bilgisini buradan gösterir.
         */
        HangmanRoundResponse lastFinishedRound,
        List<HangmanParticipantResponse> participants,
        List<HangmanLeaderboardEntry> leaderboard,
        /** Son tahminler (en yeni önce) — canlı akış için. */
        List<HangmanGuessResponse> recentGuesses,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    /**
     * @param totalRounds turlar repository'den sayılır — HangmanSession üzerinde
     *                    koleksiyon tutulmuyor (bkz. entity'deki not).
     */
    public static HangmanSessionResponse from(HangmanSession session,
                                              HangmanRoundResponse round,
                                              HangmanRoundResponse lastFinishedRound,
                                              String currentTurnName,
                                              int totalRounds,
                                              List<HangmanParticipantResponse> participants,
                                              List<HangmanLeaderboardEntry> leaderboard,
                                              List<HangmanGuessResponse> recentGuesses) {
        return new HangmanSessionResponse(
                session.getId().toString(),
                session.getTeam().getId().toString(),
                session.getHostEmail(),
                session.getHostName(),
                session.getStatus().name(),
                session.getLanguage(),
                session.getWordSource().name(),
                Boolean.TRUE.equals(session.getModeratorPlays()),
                session.getCurrentRoundIndex(),
                totalRounds,
                session.getCurrentTurnEmail(),
                currentTurnName,
                round,
                lastFinishedRound,
                participants,
                leaderboard,
                recentGuesses,
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getFinishedAt()
        );
    }
}

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
        /** Rastgele kelime kategorisi; karışık oynanıyorsa null. */
        String category,
        String categoryLabel,
        boolean moderatorPlays,
        int currentRoundIndex,
        int totalRounds,
        String currentTurnEmail,
        String currentTurnName,
        /** Bir sıranın toplam süresi (sn). Dolduğunda sıra kendiliğinden devreder. */
        int turnSeconds,
        /** Sıranın ilk kaç saniyesinde kelime tahmini yalnızca sırası gelene açık. */
        int wordLockSeconds,
        /** Sıranın bitmesine kalan saniye — sunucu saatiyle hesaplanır (istemci saat kayması olmasın). */
        int turnSecondsLeft,
        /** Kelime tahmininin herkese açılmasına kalan saniye; 0 = herkes tahmin edebilir. */
        int wordOpenInSeconds,
        /** Aktif tur — lobide ve oyun bitince null olabilir. */
        HangmanRoundResponse round,
        /**
         * En son biten tur (cevabı açık) — istemci "kelime neydi" bilgisini buradan gösterir.
         */
        HangmanRoundResponse lastFinishedRound,
        /**
         * Tur bitti, moderatörün sonraki kelimeye geçmesi bekleniyor.
         * true iken hiçbir tahmin kabul edilmez ve süre sayaçları durur.
         */
        boolean awaitingNextRound,
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
                                              int turnSeconds,
                                              int wordLockSeconds,
                                              int turnSecondsLeft,
                                              int wordOpenInSeconds,
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
                session.getCategory() == null ? null : session.getCategory().name(),
                session.getCategory() == null ? null : session.getCategory().label(session.getLanguage()),
                Boolean.TRUE.equals(session.getModeratorPlays()),
                session.getCurrentRoundIndex(),
                totalRounds,
                session.getCurrentTurnEmail(),
                currentTurnName,
                turnSeconds,
                wordLockSeconds,
                turnSecondsLeft,
                wordOpenInSeconds,
                round,
                lastFinishedRound,
                Boolean.TRUE.equals(session.getAwaitingNextRound()),
                participants,
                leaderboard,
                recentGuesses,
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getFinishedAt()
        );
    }
}

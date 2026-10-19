package com.scrumtools.dto;

import com.scrumtools.entity.HangmanMode;
import com.scrumtools.entity.HangmanPace;
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
        /**
         * Bir sıranın toplam süresi (sn). Dolduğunda sıra kendiliğinden devreder.
         * Yarış modunda bir kelimenin çözülmesi için tanınan süredir.
         */
        int turnSeconds,
        /** Sıranın bitmesine kalan saniye — sunucu saatiyle hesaplanır (istemci saat kayması olmasın). */
        int turnSecondsLeft,
        /** Oyun modu: TURN | RACE | TEAM. */
        String mode,
        /** Oyun temposu: FAST | NORMAL | RELAXED. */
        String pace,
        /** Sırası gelen tek sırada en fazla kaç doğru harf bulabilir; sonra sıra devreder. */
        int maxStreak,
        /** Tur arası geri sayımla kendiliğinden kapanıyor mu (moderatör ayarı). */
        boolean autoAdvance,
        /**
         * Ara ekranının kendiliğinden kapanmasına kalan saniye.
         * null = geri sayım yok (otomatik geçiş kapalı, tur arasında değil ya da duraklatıldı).
         */
        Integer intermissionSecondsLeft,
        /** Tahmin oylamasında isabet başına verilen puan. */
        int predictionScore,
        /** Şu an bekleyen gizli tahmin sayısı — kimin neyi seçtiği yayınlanmaz. */
        int predictionCount,
        /** Sırası değilken yanlış kelime tahmininin puan etkisi (negatif). */
        int wrongWordScore,
        /** Takım modunda sırası gelen takım (1 | 2); diğer modlarda null. */
        Integer currentTeam,
        /** Takım modunda iki takımın özeti; diğer modlarda boş. */
        List<HangmanTeamResponse> teams,
        /** Takım modunda sırası gelen takımın harf oyları (en çok oy alan önce); diğer modlarda boş. */
        List<HangmanTeamVoteTally> teamVotes,
        /** Yarış modunda oyuncuların ilerlemesi (önce bitirenler); diğer modlarda boş. */
        List<HangmanRaceProgress> raceProgress,
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
                                              int turnSecondsLeft,
                                              int maxStreak,
                                              Integer intermissionSecondsLeft,
                                              int predictionScore,
                                              int predictionCount,
                                              int wrongWordScore,
                                              List<HangmanTeamResponse> teams,
                                              List<HangmanTeamVoteTally> teamVotes,
                                              List<HangmanRaceProgress> raceProgress,
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
                turnSecondsLeft,
                HangmanMode.of(session.getMode()).name(),
                HangmanPace.of(session.getPace()).name(),
                maxStreak,
                Boolean.TRUE.equals(session.getAutoAdvance()),
                intermissionSecondsLeft,
                predictionScore,
                predictionCount,
                wrongWordScore,
                session.getCurrentTeam(),
                teams,
                teamVotes,
                raceProgress,
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

package com.scrumtools.dto;

import com.scrumtools.entity.HangmanRaceBoard;
import com.scrumtools.entity.HangmanRound;
import com.scrumtools.entity.HangmanRoundStatus;

import java.util.List;

/**
 * Yarış modunda oyuncunun KENDİ tahtası — yalnızca sahibine döner, yayınlanmaz.
 *
 * GÜVENLİK: Cevap burada da maskelenir; {@code maskedWord} yalnızca bu oyuncunun açtığı
 * harfleri gösterir. Tahta ilk tahminde oluşturulur — henüz tahmin yoksa boş tahta döner.
 */
public record HangmanRaceBoardResponse(
        String roundId,
        int roundOrder,
        String maskedWord,
        int wordLength,
        List<String> guessedLetters,
        List<String> wrongLetters,
        int wrongCount,
        int maxWrong,
        /** ACTIVE | SOLVED | FAILED */
        String status,
        /** Çözenler arasındaki sıran (1 = ilk); çözmediysen null. */
        Integer finishOrder,
        int scoreEarned
) {

    /** @param board null olabilir — oyuncu bu turda henüz tahmin yapmadı. */
    public static HangmanRaceBoardResponse from(HangmanRound round, HangmanRaceBoard board, int maxWrong) {
        String word = round.getWord();
        List<String> guessed = board == null ? List.of() : List.copyOf(board.getGuessedLetters());

        StringBuilder masked = new StringBuilder(word.length());
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            masked.append(guessed.contains(ch) ? ch : "_");
        }

        return new HangmanRaceBoardResponse(
                round.getId().toString(),
                round.getRoundOrder(),
                masked.toString(),
                word.length(),
                guessed,
                guessed.stream().filter(l -> !word.contains(l)).toList(),
                board == null ? 0 : board.getWrongCount(),
                maxWrong,
                board == null ? HangmanRoundStatus.ACTIVE.name() : board.getStatus().name(),
                board == null ? null : board.getFinishOrder(),
                board == null ? 0 : board.getScoreEarned()
        );
    }
}

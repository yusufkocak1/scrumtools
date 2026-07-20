package com.scrumtools.dto;

import com.scrumtools.entity.HangmanRound;
import com.scrumtools.entity.HangmanRoundStatus;

import java.util.List;

/**
 * Oynanan turun istemciye açık hâli.
 *
 * GÜVENLİK: {@code revealedWord} yalnızca tur bittiğinde (SOLVED/FAILED) doldurulur.
 * Tur oynanırken istemciye sadece {@code maskedWord} gider — bu sayede oyuncular
 * ağ trafiğinden cevabı okuyamaz. Bu kuralı bozmadan alan eklemeyin.
 */
public record HangmanRoundResponse(
        String id,
        int roundOrder,
        /** Bilinen harfler açık, bilinmeyenler '_' — örn. "_e__l_p" */
        String maskedWord,
        int wordLength,
        List<String> guessedLetters,
        List<String> wrongLetters,
        int wrongCount,
        int maxWrong,
        String status,
        /** Sadece tur bittiğinde dolu; aksi hâlde null. */
        String revealedWord,
        String solvedByEmail,
        String solvedByName
) {

    public static HangmanRoundResponse from(HangmanRound round, int maxWrong) {
        String word = round.getWord();
        List<String> guessed = round.getGuessedLetters();

        StringBuilder masked = new StringBuilder(word.length());
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            masked.append(guessed.contains(ch) ? ch : "_");
        }

        List<String> wrongLetters = guessed.stream()
                .filter(l -> !word.contains(l))
                .toList();

        boolean finished = round.getStatus() == HangmanRoundStatus.SOLVED
                || round.getStatus() == HangmanRoundStatus.FAILED;

        return new HangmanRoundResponse(
                round.getId().toString(),
                round.getRoundOrder(),
                masked.toString(),
                word.length(),
                List.copyOf(guessed),
                wrongLetters,
                round.getWrongCount(),
                maxWrong,
                round.getStatus().name(),
                finished ? word : null,
                round.getSolvedByEmail(),
                round.getSolvedByName()
        );
    }
}

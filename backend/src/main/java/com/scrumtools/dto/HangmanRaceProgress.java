package com.scrumtools.dto;

/**
 * Yarış modunda bir oyuncunun herkese yayınlanan ilerlemesi.
 * Hangi harfleri açtığı bilinçli olarak YOK — yalnızca kaç harf açtığı.
 */
public record HangmanRaceProgress(
        String email,
        String displayName,
        /** Kelimede açılmış harf (konum) sayısı. */
        int revealedCount,
        int wordLength,
        int wrongCount,
        int maxWrong,
        /** ACTIVE (henüz tahmin yapmadıysa da) | SOLVED | FAILED */
        String status,
        Integer finishOrder,
        int scoreEarned
) {
}

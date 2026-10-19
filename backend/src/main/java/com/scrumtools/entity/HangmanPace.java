package com.scrumtools.entity;

import java.util.Locale;

/**
 * Adam Asmaca oyun temposu — moderatör oturumu açarken seçer.
 *
 * Sıra süresi bekleyen oyuncunun en fazla ne kadar beklediğini belirler; kalabalık
 * ekiplerde {@link #FAST} önerilir. Kelime kilidi, sıranın ilk kaç saniyesinde kelime
 * tahmininin yalnızca sırası gelene açık olduğudur.
 */
public enum HangmanPace {

    FAST(15, 5),
    NORMAL(30, 10),
    RELAXED(45, 15);

    private final int turnSeconds;
    private final int wordLockSeconds;

    HangmanPace(int turnSeconds, int wordLockSeconds) {
        this.turnSeconds = turnSeconds;
        this.wordLockSeconds = wordLockSeconds;
    }

    public int turnSeconds() {
        return turnSeconds;
    }

    public int wordLockSeconds() {
        return wordLockSeconds;
    }

    /** Boş/tanınmayan değerde {@link #NORMAL} döner. */
    public static HangmanPace parse(String raw) {
        if (raw == null || raw.isBlank()) return NORMAL;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }

    /** Tempo kolonu eklenmeden önce açılmış oturumlarda değer null'dır. */
    public static HangmanPace of(HangmanPace pace) {
        return pace == null ? NORMAL : pace;
    }
}

package com.scrumtools.entity;

import java.util.Locale;

/**
 * Adam Asmaca oyun temposu — moderatör oturumu açarken seçer.
 *
 * Sıra süresi bekleyen oyuncunun en fazla ne kadar beklediğini belirler; kalabalık
 * ekiplerde {@link #FAST} önerilir. Yarış modunda sıra yoktur; orada bir kelimenin
 * çözülmesi için tanınan süre ({@link #raceSeconds()}) kullanılır.
 */
public enum HangmanPace {

    FAST(15, 60),
    NORMAL(30, 90),
    RELAXED(45, 120);

    private final int turnSeconds;
    private final int raceSeconds;

    HangmanPace(int turnSeconds, int raceSeconds) {
        this.turnSeconds = turnSeconds;
        this.raceSeconds = raceSeconds;
    }

    public int turnSeconds() {
        return turnSeconds;
    }

    public int raceSeconds() {
        return raceSeconds;
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

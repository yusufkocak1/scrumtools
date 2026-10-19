package com.scrumtools.entity;

import java.util.Locale;

/**
 * Adam Asmaca oyun modu — moderatör oturumu açarken seçer.
 */
public enum HangmanMode {

    /** Klasik: ortak kelime, oyuncular sırayla tahmin eder. */
    TURN,
    /**
     * Yarış: herkes aynı kelimeyi kendi tahtasında AYNI ANDA çözer; kimse beklemez.
     * Önce bitiren daha çok puan alır. Kalabalık ekipler için.
     */
    RACE,
    /**
     * Takım: oyuncular iki takıma ayrılır, sıra takımlar arasında geçer. Sırası gelen
     * takımın üyeleri harfe oylamayla karar verir — herkes her sırada konuşmaya katılır.
     */
    TEAM;

    /** Boş/tanınmayan değerde {@link #TURN} döner. */
    public static HangmanMode parse(String raw) {
        if (raw == null || raw.isBlank()) return TURN;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return TURN;
        }
    }

    /** Mod kolonu eklenmeden önce açılmış oturumlarda değer null'dır. */
    public static HangmanMode of(HangmanMode mode) {
        return mode == null ? TURN : mode;
    }
}

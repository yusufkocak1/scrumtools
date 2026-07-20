package com.scrumtools.entity;

/**
 * Oturumdaki kelimelerin nereden geldiği.
 *
 * CUSTOM seçildiğinde moderatör kelimeleri kendisi girer; cevapları bildiği için
 * o oturumda oyuncu olamaz (sadece sunucu/izleyici olur). Bkz. HangmanSessionService.
 */
public enum HangmanWordSource {
    /** Kelimeler DB havuzundan (+ dahili havuz) rastgele çekilir */
    RANDOM,
    /** Kelimeleri moderatör elle girer */
    CUSTOM
}

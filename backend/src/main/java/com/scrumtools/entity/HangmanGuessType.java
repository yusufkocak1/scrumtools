package com.scrumtools.entity;

/**
 * Tahmin tipi.
 *
 * LETTER → yanlışsa adam asılır ve sıra devreder.
 * WORD   → yanlışsa adam ASILMAZ, sadece sıra devreder (riski daha düşük, ödülü daha yüksek).
 */
public enum HangmanGuessType {
    LETTER,
    WORD
}

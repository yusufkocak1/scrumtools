package com.scrumtools.entity;

/**
 * Bir turun (tek kelimenin) durumu.
 */
public enum HangmanRoundStatus {
    /** Sırası gelmedi */
    PENDING,
    /** Şu an oynanıyor */
    ACTIVE,
    /** Kelime bulundu */
    SOLVED,
    /** Adam asıldı, kelime bulunamadı */
    FAILED
}

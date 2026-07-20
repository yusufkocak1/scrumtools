package com.scrumtools.entity;

/**
 * Adam Asmaca takım oturumu durumları.
 */
public enum HangmanSessionStatus {
    /** Katılımcılar bekleniyor */
    LOBBY,
    /** Oyun devam ediyor */
    IN_PROGRESS,
    /** Oyun tamamlandı */
    FINISHED
}

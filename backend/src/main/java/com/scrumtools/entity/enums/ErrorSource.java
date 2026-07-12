package com.scrumtools.entity.enums;

public enum ErrorSource {
    BACKEND,   // Sunucu tarafı (5xx) hataları
    FRONTEND   // Tarayıcıda yakalanmayan JS hataları
}

package com.scrumtools.dto;

/**
 * Oyun kurulumunda listelenen kategori.
 *
 * @param wordCount dahili havuz + DB'deki kelimelerin toplamı (tekrarlar sayılmaz)
 */
public record HangmanCategoryResponse(
        String code,
        String label,
        int wordCount
) {}

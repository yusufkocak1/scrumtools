package com.scrumtools.dto;

import com.scrumtools.entity.HangmanWord;

import java.time.LocalDateTime;

public record HangmanWordResponse(
        String id,
        String word,
        String language,
        /** Kategori özelliğinden önce eklenmiş kayıtlarda null olabilir. */
        String category,
        String categoryLabel,
        String createdByEmail,
        LocalDateTime createdAt
) {
    public static HangmanWordResponse from(HangmanWord w) {
        return new HangmanWordResponse(
                w.getId().toString(),
                w.getWord(),
                w.getLanguage(),
                w.getCategory() == null ? null : w.getCategory().name(),
                w.getCategory() == null ? null : w.getCategory().label(w.getLanguage()),
                w.getCreatedByEmail(),
                w.getCreatedAt()
        );
    }
}

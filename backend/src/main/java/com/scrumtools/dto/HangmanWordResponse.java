package com.scrumtools.dto;

import com.scrumtools.entity.HangmanCategory;
import com.scrumtools.entity.HangmanWord;

import java.time.LocalDateTime;

/**
 * @param id     dahili (kod içindeki) kelimelerde null — bunlar silinemez
 * @param source BUILT_IN = dahili havuz, CUSTOM = admin tarafından eklenmiş
 */
public record HangmanWordResponse(
        String id,
        String word,
        String language,
        /** Kategori özelliğinden önce eklenmiş kayıtlarda null olabilir. */
        String category,
        String categoryLabel,
        String source,
        String createdByEmail,
        LocalDateTime createdAt
) {
    public static final String SOURCE_BUILT_IN = "BUILT_IN";
    public static final String SOURCE_CUSTOM = "CUSTOM";

    public static HangmanWordResponse from(HangmanWord w) {
        return new HangmanWordResponse(
                w.getId().toString(),
                w.getWord(),
                w.getLanguage(),
                w.getCategory() == null ? null : w.getCategory().name(),
                w.getCategory() == null ? null : w.getCategory().label(w.getLanguage()),
                SOURCE_CUSTOM,
                w.getCreatedByEmail(),
                w.getCreatedAt()
        );
    }

    /** Dahili havuzdaki kelime — DB kaydı yoktur, bu yüzden id/ekleyen/tarih boştur. */
    public static HangmanWordResponse builtIn(String word, String language, HangmanCategory category) {
        return new HangmanWordResponse(
                null,
                word,
                language,
                category.name(),
                category.label(language),
                SOURCE_BUILT_IN,
                null,
                null
        );
    }
}

package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Toplu kelime ekleme. Kelimeler tek kategoriye, iki dil için birlikte eklenir;
 * diller bağımsız havuzlardır, listelerin aynı uzunlukta veya birbirinin çevirisi
 * olması gerekmez. En az birinin dolu olması yeterlidir.
 *
 * @param category HangmanCategory kodu (ör. ANIMALS)
 */
public record HangmanWordBulkRequest(
        @NotBlank String category,
        List<String> trWords,
        List<String> enWords
) {}

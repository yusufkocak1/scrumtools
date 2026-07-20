package com.scrumtools.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Adam Asmaca takım oturumu başlatma isteği.
 *
 * customWords doluysa kelime kaynağı CUSTOM sayılır ve moderatör oyuncu olamaz
 * (cevapları bildiği için). Boşsa kelimeler DB + dahili havuzdan rastgele çekilir.
 */
public record HangmanSessionStartRequest(
        @NotBlank String language,

        /** Kaç kelime oynanacak. customWords doluysa yok sayılır. */
        @Min(1) @Max(20) Integer roundCount,

        /** Moderatörün kendi belirlediği kelimeler. Boş/null ise rastgele çekilir. */
        List<String> customWords,

        /** Moderatör de sıraya girsin mi? customWords doluysa yok sayılır (false'a zorlanır). */
        Boolean moderatorPlays
) {
}

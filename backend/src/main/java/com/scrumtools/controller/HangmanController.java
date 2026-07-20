package com.scrumtools.controller;

import com.scrumtools.dto.HangmanWordResponse;
import com.scrumtools.service.HangmanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adam Asmaca kelime havuzu — okuma (oyun içi kullanım).
 * Kelimeler global'dir; eklenip silinmesi sadece SUPER_ADMIN'e özeldir
 * (bkz. AdminHangmanController).
 *
 *   GET /api/hangman/words?language=tr|en → global kelime listesi
 */
@RestController
@RequestMapping("/api/hangman")
@RequiredArgsConstructor
public class HangmanController {

    private final HangmanService hangmanService;

    @GetMapping("/words")
    public ResponseEntity<List<HangmanWordResponse>> getWords(@RequestParam(defaultValue = "tr") String language) {
        return ResponseEntity.ok(hangmanService.getWords(language));
    }
}

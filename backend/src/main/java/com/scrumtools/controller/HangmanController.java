package com.scrumtools.controller;

import com.scrumtools.dto.HangmanCategoryResponse;
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
 *   GET /api/hangman/categories?language=tr|en        → kategoriler + kelime sayıları
 *   GET /api/hangman/words?language=tr|en&category=.. → kelime listesi (kategori opsiyonel)
 */
@RestController
@RequestMapping("/api/hangman")
@RequiredArgsConstructor
public class HangmanController {

    private final HangmanService hangmanService;

    @GetMapping("/categories")
    public ResponseEntity<List<HangmanCategoryResponse>> getCategories(
            @RequestParam(defaultValue = "tr") String language) {
        return ResponseEntity.ok(hangmanService.getCategories(language));
    }

    @GetMapping("/words")
    public ResponseEntity<List<HangmanWordResponse>> getWords(
            @RequestParam(defaultValue = "tr") String language,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(hangmanService.getWords(language, category));
    }
}

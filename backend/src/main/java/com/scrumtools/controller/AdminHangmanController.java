package com.scrumtools.controller;

import com.scrumtools.dto.HangmanWordBulkRequest;
import com.scrumtools.dto.HangmanWordBulkResponse;
import com.scrumtools.dto.HangmanWordPageResponse;
import com.scrumtools.service.HangmanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Adam Asmaca kelime havuzu yönetimi — sadece SUPER_ADMIN.
 *
 *   GET    /api/admin/hangman/words?language=tr|en&category=..&search=..&page=0&size=50
 *          → dahili havuz + eklenen kelimeler, sayfalı
 *   POST   /api/admin/hangman/words          → toplu kelime ekle (kategori zorunlu)
 *   DELETE /api/admin/hangman/words/{wordId} → kelime sil (yalnızca eklenen kelimeler)
 */
@RestController
@RequestMapping("/api/admin/hangman")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isSuperAdmin(authentication)")
public class AdminHangmanController {

    private final HangmanService hangmanService;

    @GetMapping("/words")
    public ResponseEntity<HangmanWordPageResponse> getWords(
            @RequestParam(defaultValue = "tr") String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(hangmanService.getWordPage(language, category, search, page, size));
    }

    @PostMapping("/words")
    public ResponseEntity<HangmanWordBulkResponse> addWords(@Valid @RequestBody HangmanWordBulkRequest request) {
        return ResponseEntity.ok(hangmanService.addWords(request));
    }

    @DeleteMapping("/words/{wordId}")
    public ResponseEntity<Void> deleteWord(@PathVariable UUID wordId) {
        hangmanService.deleteWord(wordId);
        return ResponseEntity.noContent().build();
    }
}

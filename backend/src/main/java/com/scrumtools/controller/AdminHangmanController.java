package com.scrumtools.controller;

import com.scrumtools.dto.HangmanWordBulkRequest;
import com.scrumtools.dto.HangmanWordBulkResponse;
import com.scrumtools.dto.HangmanWordResponse;
import com.scrumtools.service.HangmanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Adam Asmaca kelime havuzu yönetimi — sadece SUPER_ADMIN.
 *
 *   GET    /api/admin/hangman/words?language=tr|en → kelime listesi
 *   POST   /api/admin/hangman/words                → toplu kelime ekle
 *   DELETE /api/admin/hangman/words/{wordId}       → kelime sil
 */
@RestController
@RequestMapping("/api/admin/hangman")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isSuperAdmin(authentication)")
public class AdminHangmanController {

    private final HangmanService hangmanService;

    @GetMapping("/words")
    public ResponseEntity<List<HangmanWordResponse>> getWords(@RequestParam(defaultValue = "tr") String language) {
        return ResponseEntity.ok(hangmanService.getWords(language));
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

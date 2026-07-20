package com.scrumtools.controller;

import com.scrumtools.dto.HangmanWordBulkRequest;
import com.scrumtools.dto.HangmanWordBulkResponse;
import com.scrumtools.dto.HangmanWordResponse;
import com.scrumtools.service.HangmanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Adam Asmaca takım kelime havuzu REST Controller.
 *
 *   GET    /api/teams/{teamId}/hangman/words?language=tr|en   → takımın o dildeki özel kelimeleri
 *   POST   /api/teams/{teamId}/hangman/words                  → toplu kelime ekle
 *   DELETE /api/teams/{teamId}/hangman/words/{wordId}         → kelime sil
 */
@RestController
@RequestMapping("/api/teams/{teamId}/hangman")
@RequiredArgsConstructor
public class HangmanController {

    private final HangmanService hangmanService;

    @GetMapping("/words")
    public ResponseEntity<List<HangmanWordResponse>> getWords(@PathVariable UUID teamId,
                                                               @RequestParam(defaultValue = "tr") String language) {
        return ResponseEntity.ok(hangmanService.getWords(teamId, language));
    }

    @PostMapping("/words")
    public ResponseEntity<HangmanWordBulkResponse> addWords(@PathVariable UUID teamId,
                                                             @Valid @RequestBody HangmanWordBulkRequest request) {
        return ResponseEntity.ok(hangmanService.addWords(teamId, request));
    }

    @DeleteMapping("/words/{wordId}")
    public ResponseEntity<Void> deleteWord(@PathVariable UUID teamId, @PathVariable UUID wordId) {
        hangmanService.deleteWord(teamId, wordId);
        return ResponseEntity.noContent().build();
    }
}

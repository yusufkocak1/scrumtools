package com.scrumtools.controller;

import com.scrumtools.dto.HangmanGuessRequest;
import com.scrumtools.dto.HangmanSessionResponse;
import com.scrumtools.dto.HangmanSessionStartRequest;
import com.scrumtools.service.HangmanSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Adam Asmaca takım oyunu.
 *
 * WebSocket stratejisi: Data-Carrying
 *   /topic/hangman/{teamId}/state → oturumun tam durumu (lobi, aktif tur, sıra, puan tablosu)
 *
 *   POST   /api/teams/{teamId}/hangman/sessions                  → oturum aç (moderatör olursun)
 *   GET    /api/teams/{teamId}/hangman/sessions/active           → aktif oturum
 *   GET    /api/teams/{teamId}/hangman/sessions/history          → tamamlanmış oyunlar
 *   GET    /api/teams/{teamId}/hangman/sessions/{id}             → oturum detayı
 *   POST   /api/teams/{teamId}/hangman/sessions/{id}/join        → oyuna katıl
 *   POST   /api/teams/{teamId}/hangman/sessions/{id}/begin       → oyunu başlat (moderatör)
 *   POST   /api/teams/{teamId}/hangman/sessions/{id}/guess-letter→ harf tahmini
 *   POST   /api/teams/{teamId}/hangman/sessions/{id}/guess-word  → kelime tahmini
 *   POST   /api/teams/{teamId}/hangman/sessions/{id}/skip-turn   → sırayı devret (moderatör)
 *   POST   /api/teams/{teamId}/hangman/sessions/{id}/finish      → oyunu bitir (moderatör)
 */
@RestController
@RequestMapping("/api/teams/{teamId}/hangman")
@RequiredArgsConstructor
public class HangmanSessionController {

    private final HangmanSessionService hangmanSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<HangmanSessionResponse> startSession(@PathVariable UUID teamId,
                                                               @Valid @RequestBody HangmanSessionStartRequest request) {
        return ResponseEntity.ok(hangmanSessionService.startSession(teamId, request));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<HangmanSessionResponse> getActiveSession(@PathVariable UUID teamId) {
        HangmanSessionResponse session = hangmanSessionService.getActiveSession(teamId);
        return session == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/history")
    public ResponseEntity<List<HangmanSessionResponse>> getHistory(@PathVariable UUID teamId) {
        return ResponseEntity.ok(hangmanSessionService.getHistory(teamId));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<HangmanSessionResponse> getSession(@PathVariable UUID teamId,
                                                             @PathVariable UUID sessionId) {
        return ResponseEntity.ok(hangmanSessionService.getSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/join")
    public ResponseEntity<HangmanSessionResponse> join(@PathVariable UUID teamId,
                                                       @PathVariable UUID sessionId) {
        return ResponseEntity.ok(hangmanSessionService.joinSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/begin")
    public ResponseEntity<HangmanSessionResponse> begin(@PathVariable UUID teamId,
                                                        @PathVariable UUID sessionId) {
        return ResponseEntity.ok(hangmanSessionService.beginGame(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/guess-letter")
    public ResponseEntity<HangmanSessionResponse> guessLetter(@PathVariable UUID teamId,
                                                              @PathVariable UUID sessionId,
                                                              @Valid @RequestBody HangmanGuessRequest request) {
        return ResponseEntity.ok(hangmanSessionService.guessLetter(sessionId, request.value()));
    }

    @PostMapping("/sessions/{sessionId}/guess-word")
    public ResponseEntity<HangmanSessionResponse> guessWord(@PathVariable UUID teamId,
                                                            @PathVariable UUID sessionId,
                                                            @Valid @RequestBody HangmanGuessRequest request) {
        return ResponseEntity.ok(hangmanSessionService.guessWord(sessionId, request.value()));
    }

    @PostMapping("/sessions/{sessionId}/skip-turn")
    public ResponseEntity<HangmanSessionResponse> skipTurn(@PathVariable UUID teamId,
                                                           @PathVariable UUID sessionId) {
        return ResponseEntity.ok(hangmanSessionService.skipTurn(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public ResponseEntity<HangmanSessionResponse> finish(@PathVariable UUID teamId,
                                                         @PathVariable UUID sessionId) {
        return ResponseEntity.ok(hangmanSessionService.finishSession(sessionId));
    }
}

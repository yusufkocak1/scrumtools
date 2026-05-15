package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quiz REST Controller
 *
 * WebSocket stratejisi: Data-Carrying
 *   /topic/quiz/{teamId}/state     → oturum durumu (lobby, aktif soru, leaderboard, bitiş)
 *   /topic/quiz/{teamId}/answered  → cevaplayan sayısı güncellemesi
 *
 * Template CRUD:
 *   GET    /api/teams/{teamId}/quiz/templates           → şablon listesi
 *   GET    /api/teams/{teamId}/quiz/templates/{id}      → şablon detayı
 *   POST   /api/teams/{teamId}/quiz/templates           → şablon oluştur
 *   PUT    /api/teams/{teamId}/quiz/templates/{id}      → şablon güncelle
 *   DELETE /api/teams/{teamId}/quiz/templates/{id}      → şablon sil
 *
 * Session:
 *   POST   /api/teams/{teamId}/quiz/sessions                   → oturum başlat
 *   GET    /api/teams/{teamId}/quiz/sessions/active             → aktif oturum
 *   GET    /api/teams/{teamId}/quiz/sessions/history            → geçmiş oturumlar
 *   GET    /api/teams/{teamId}/quiz/sessions/{id}               → oturum detayı
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/join          → oturuma katıl
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/next          → sonraki soru
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/answer        → cevap gönder
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/show-result   → soru sonucunu göster
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/finish        → oturumu bitir
 *   GET    /api/teams/{teamId}/quiz/sessions/{id}/report        → oturum raporu
 */
@RestController
@RequestMapping("/api/teams/{teamId}/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // ─── Template CRUD ──────────────────────────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<List<QuizTemplateResponse>> getTemplates(@PathVariable UUID teamId) {
        return ResponseEntity.ok(quizService.getTemplates(teamId));
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<QuizTemplateResponse> getTemplate(@PathVariable UUID teamId,
                                                            @PathVariable UUID templateId) {
        return ResponseEntity.ok(quizService.getTemplate(templateId));
    }

    @PostMapping("/templates")
    public ResponseEntity<QuizTemplateResponse> createTemplate(@PathVariable UUID teamId,
                                                               @Valid @RequestBody QuizTemplateRequest request) {
        return ResponseEntity.ok(quizService.createTemplate(teamId, request));
    }

    @PutMapping("/templates/{templateId}")
    public ResponseEntity<QuizTemplateResponse> updateTemplate(@PathVariable UUID teamId,
                                                               @PathVariable UUID templateId,
                                                               @Valid @RequestBody QuizTemplateRequest request) {
        return ResponseEntity.ok(quizService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID teamId,
                                               @PathVariable UUID templateId) {
        quizService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    // ─── Session ────────────────────────────────────────────────────────────────

    @PostMapping("/sessions")
    public ResponseEntity<QuizSessionResponse> startSession(@PathVariable UUID teamId,
                                                            @RequestBody Map<String, String> body) {
        UUID templateId = UUID.fromString(body.get("templateId"));
        return ResponseEntity.ok(quizService.startSession(teamId, templateId));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<QuizSessionResponse> getActiveSession(@PathVariable UUID teamId) {
        QuizSessionResponse session = quizService.getActiveSession(teamId);
        if (session == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/history")
    public ResponseEntity<List<QuizSessionResponse>> getSessionHistory(@PathVariable UUID teamId) {
        return ResponseEntity.ok(quizService.getSessionHistory(teamId));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<QuizSessionResponse> getSession(@PathVariable UUID teamId,
                                                          @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.getSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/join")
    public ResponseEntity<QuizSessionResponse> joinSession(@PathVariable UUID teamId,
                                                           @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.joinSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/next")
    public ResponseEntity<QuizSessionResponse> nextQuestion(@PathVariable UUID teamId,
                                                            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.nextQuestion(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/answer")
    public ResponseEntity<QuizAnswerResponse> submitAnswer(@PathVariable UUID teamId,
                                                           @PathVariable UUID sessionId,
                                                           @Valid @RequestBody QuizAnswerRequest request) {
        return ResponseEntity.ok(quizService.submitAnswer(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/show-result")
    public ResponseEntity<QuizSessionResponse> showQuestionResult(@PathVariable UUID teamId,
                                                                  @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.showQuestionResult(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public ResponseEntity<QuizSessionResponse> finishSession(@PathVariable UUID teamId,
                                                             @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.finishSession(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getSessionReport(@PathVariable UUID teamId,
                                                                @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.getSessionReport(sessionId));
    }
}


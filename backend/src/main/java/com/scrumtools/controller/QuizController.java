package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
 * Görsel:
 *   POST   /api/teams/{teamId}/quiz/images              → soru görseli yükle
 *
 * Session:
 *   POST   /api/teams/{teamId}/quiz/sessions                   → oturum başlat (moderatorMode)
 *   GET    /api/teams/{teamId}/quiz/sessions/active             → aktif oturumlar (liste)
 *   GET    /api/teams/{teamId}/quiz/sessions/history            → geçmiş oturumlar
 *   GET    /api/teams/{teamId}/quiz/sessions/{id}               → oturum detayı
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/join          → oturuma katıl
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/next          → sonraki soru
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/answer        → cevap gönder
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/show-result   → soru sonucunu göster
 *   GET    /api/teams/{teamId}/quiz/sessions/{id}/moderator     → moderatör paneli (host'a özel)
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/finish        → oturumu bitir
 *   POST   /api/teams/{teamId}/quiz/sessions/{id}/cancel        → başlamamış lobiyi kapat
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

    // ─── Soru Görselleri ────────────────────────────────────────────────────────

    @PostMapping("/images")
    public ResponseEntity<QuizImageResponse> uploadImage(@PathVariable UUID teamId,
                                                         @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(quizService.uploadImage(teamId, file));
    }

    // ─── Session ────────────────────────────────────────────────────────────────

    @PostMapping("/sessions")
    public ResponseEntity<QuizSessionResponse> startSession(@PathVariable UUID teamId,
                                                            @RequestBody Map<String, Object> body) {
        UUID templateId = UUID.fromString(String.valueOf(body.get("templateId")));
        // Belirtilmezse moderatör modu açık — oturumu başlatan kişi sunar, yarışmaz.
        boolean moderatorMode = !Boolean.FALSE.equals(body.get("moderatorMode"));
        return ResponseEntity.ok(quizService.startSession(teamId, templateId, moderatorMode));
    }

    /** Takımdaki tüm aktif oturumlar — paralel yarışmalar mümkün. */
    @GetMapping("/sessions/active")
    public ResponseEntity<List<QuizSessionResponse>> getActiveSessions(@PathVariable UUID teamId) {
        return ResponseEntity.ok(quizService.getActiveSessions(teamId));
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

    @GetMapping("/sessions/{sessionId}/moderator")
    public ResponseEntity<QuizModeratorViewResponse> getModeratorView(@PathVariable UUID teamId,
                                                                      @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.getModeratorView(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public ResponseEntity<QuizSessionResponse> finishSession(@PathVariable UUID teamId,
                                                             @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.finishSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    public ResponseEntity<QuizSessionResponse> cancelSession(@PathVariable UUID teamId,
                                                             @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.cancelSession(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getSessionReport(@PathVariable UUID teamId,
                                                                @PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.getSessionReport(sessionId));
    }
}


package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Quiz servisi — Kahoot benzeri yarışma yönetimi.
 *
 * WebSocket stratejisi: Data-Carrying
 *   /topic/quiz/{teamId}/state       → oturum durumu (lobby, soru, leaderboard, bitiş)
 *   /topic/quiz/{teamId}/answered    → bir kullanıcı cevap verdiğinde (cevap sayısı güncellemesi)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizTemplateRepository templateRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizSessionRepository sessionRepository;
    private final QuizParticipantRepository participantRepository;
    private final QuizAnswerRepository answerRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Template CRUD ──────────────────────────────────────────────────────────

    public List<QuizTemplateResponse> getTemplates(UUID teamId) {
        return templateRepository.findByTeamIdOrderByCreatedAtDesc(teamId)
                .stream().map(QuizTemplateResponse::summary).toList();
    }

    public QuizTemplateResponse getTemplate(UUID templateId) {
        QuizTemplate t = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        return QuizTemplateResponse.from(t);
    }

    @Transactional
    public QuizTemplateResponse createTemplate(UUID teamId, QuizTemplateRequest request) {
        String email = currentEmail();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        String displayName = teamMemberRepository.findByTeamIdAndEmail(teamId, email)
                .map(TeamMember::getDisplayName).orElse(email);

        QuizTemplate template = QuizTemplate.builder()
                .team(team)
                .title(request.title())
                .description(request.description())
                .createdByEmail(email)
                .createdByName(displayName)
                .build();

        // Soruları ekle
        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 0; i < request.questions().size(); i++) {
            QuizQuestionRequest qr = request.questions().get(i);
            QuizQuestion q = QuizQuestion.builder()
                    .template(template)
                    .questionOrder(i)
                    .questionText(qr.questionText())
                    .options(new ArrayList<>(qr.options()))
                    .correctOptionIndex(qr.correctOptionIndex())
                    .timeLimitSeconds(qr.timeLimitSeconds())
                    .build();
            questions.add(q);
        }
        template.setQuestions(questions);
        templateRepository.save(template);
        return QuizTemplateResponse.from(template);
    }

    @Transactional
    public QuizTemplateResponse updateTemplate(UUID templateId, QuizTemplateRequest request) {
        QuizTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setTitle(request.title());
        template.setDescription(request.description());

        // Önce mevcut soruların cevaplarını sil (FK constraint ihlalini önlemek için)
        if (!template.getQuestions().isEmpty()) {
            answerRepository.deleteByQuestionIn(template.getQuestions());
        }
        template.getQuestions().clear();

        for (int i = 0; i < request.questions().size(); i++) {
            QuizQuestionRequest qr = request.questions().get(i);
            QuizQuestion q = QuizQuestion.builder()
                    .template(template)
                    .questionOrder(i)
                    .questionText(qr.questionText())
                    .options(new ArrayList<>(qr.options()))
                    .correctOptionIndex(qr.correctOptionIndex())
                    .timeLimitSeconds(qr.timeLimitSeconds())
                    .build();
            template.getQuestions().add(q);
        }
        templateRepository.save(template);
        return QuizTemplateResponse.from(template);
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        QuizTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Önce soruların cevaplarını sil (FK constraint ihlalini önlemek için)
        if (!template.getQuestions().isEmpty()) {
            answerRepository.deleteByQuestionIn(template.getQuestions());
        }
        templateRepository.delete(template);
    }

    // ─── Session Yönetimi ───────────────────────────────────────────────────────

    /**
     * Yeni quiz oturumu başlatır (LOBBY durumunda).
     */
    @Transactional
    public QuizSessionResponse startSession(UUID teamId, UUID templateId) {
        String email = currentEmail();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        QuizTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Aktif oturum varsa hata ver
        Optional<QuizSession> activeSession = sessionRepository.findByTeamIdAndStatus(teamId, QuizSessionStatus.LOBBY);
        if (activeSession.isPresent()) {
            throw new RuntimeException("Bu takımda zaten aktif bir quiz lobby var");
        }
        Optional<QuizSession> inProgress = sessionRepository.findByTeamIdAndStatus(teamId, QuizSessionStatus.IN_PROGRESS);
        if (inProgress.isPresent()) {
            throw new RuntimeException("Bu takımda zaten devam eden bir quiz var");
        }

        String displayName = teamMemberRepository.findByTeamIdAndEmail(teamId, email)
                .map(TeamMember::getDisplayName).orElse(email);

        QuizSession session = QuizSession.builder()
                .team(team)
                .template(template)
                .hostEmail(email)
                .hostName(displayName)
                .build();
        sessionRepository.save(session);

        // Host'u otomatik olarak katılımcı olarak ekle
        QuizParticipant hostParticipant = QuizParticipant.builder()
                .session(session)
                .userEmail(email)
                .displayName(displayName)
                .build();
        participantRepository.save(hostParticipant);

        QuizSessionResponse response = buildSessionResponse(session);
        broadcastState(teamId, response);
        return response;
    }

    /**
     * Kullanıcıyı oturuma katar.
     */
    @Transactional
    public QuizSessionResponse joinSession(UUID sessionId) {
        String email = currentEmail();
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() == QuizSessionStatus.FINISHED) {
            throw new RuntimeException("Bu oturum zaten tamamlanmış");
        }

        if (participantRepository.existsBySessionIdAndUserEmail(sessionId, email)) {
            // Zaten katılmış — durumu döndür
            return buildSessionResponse(session);
        }

        String displayName = teamMemberRepository.findByTeamIdAndEmail(session.getTeam().getId(), email)
                .map(TeamMember::getDisplayName).orElse(email);

        QuizParticipant participant = QuizParticipant.builder()
                .session(session)
                .userEmail(email)
                .displayName(displayName)
                .build();
        participantRepository.save(participant);

        QuizSessionResponse response = buildSessionResponse(session);
        broadcastState(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Aktif oturumu döndürür.
     */
    public QuizSessionResponse getActiveSession(UUID teamId) {
        // Önce LOBBY, sonra IN_PROGRESS'e bak
        QuizSession session = sessionRepository.findByTeamIdAndStatus(teamId, QuizSessionStatus.LOBBY)
                .or(() -> sessionRepository.findByTeamIdAndStatus(teamId, QuizSessionStatus.IN_PROGRESS))
                .orElse(null);
        if (session == null) return null;
        return buildSessionResponse(session);
    }

    /**
     * Oturum detayını döndürür (rapor amaçlı).
     */
    public QuizSessionResponse getSession(UUID sessionId) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        return buildSessionResponse(session);
    }

    /**
     * Geçmiş tüm tamamlanmış oturumları listeler (rapor).
     */
    public List<QuizSessionResponse> getSessionHistory(UUID teamId) {
        return sessionRepository.findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, QuizSessionStatus.FINISHED)
                .stream().map(this::buildSessionResponse).toList();
    }

    // ─── Oyun Akışı ─────────────────────────────────────────────────────────────

    /**
     * Sonraki soruya geçer. İlk çağrıda oyunu başlatır.
     * Sadece host çağırabilir.
     */
    @Transactional
    public QuizSessionResponse nextQuestion(UUID sessionId) {
        String email = currentEmail();
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getHostEmail().equals(email)) {
            throw new RuntimeException("Sadece host soruları ilerletebilir");
        }

        List<QuizQuestion> questions = session.getTemplate().getQuestions();
        int nextIndex = session.getCurrentQuestionIndex() + 1;

        if (nextIndex >= questions.size()) {
            // Tüm sorular bitti → oyunu bitir
            return finishSession(session);
        }

        // İlk soru ise oyunu başlat
        if (session.getStatus() == QuizSessionStatus.LOBBY) {
            session.setStatus(QuizSessionStatus.IN_PROGRESS);
            session.setStartedAt(LocalDateTime.now());
        }

        session.setCurrentQuestionIndex(nextIndex);
        session.setQuestionStartedAtMs(System.currentTimeMillis());
        sessionRepository.save(session);

        QuizSessionResponse response = buildSessionResponse(session);
        broadcastState(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Oyuncunun cevabını alır, puanı hesaplar.
     */
    @Transactional
    public QuizAnswerResponse submitAnswer(UUID sessionId, QuizAnswerRequest request) {
        String email = currentEmail();
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Oturum aktif değil");
        }

        UUID questionId = UUID.fromString(request.questionId());
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Daha önce cevap verildi mi?
        if (answerRepository.existsBySessionIdAndQuestionIdAndUserEmail(sessionId, questionId, email)) {
            throw new RuntimeException("Bu soruyu zaten cevapladınız");
        }

        // Katılımcı kontrolü
        QuizParticipant participant = participantRepository.findBySessionIdAndUserEmail(sessionId, email)
                .orElseThrow(() -> new RuntimeException("Oturuma katılmadınız"));

        // Puan hesapla
        boolean isCorrect = request.selectedOptionIndex().equals(question.getCorrectOptionIndex());
        int score = 0;
        if (isCorrect) {
            long timeLimitMs = question.getTimeLimitSeconds() * 1000L;
            long answeredMs = Math.min(request.answeredInMs(), timeLimitMs);
            double timeRatio = Math.max(0, (timeLimitMs - answeredMs)) / (double) timeLimitMs;
            score = (int) (1000 * timeRatio);
            score = Math.max(score, 100); // minimum 100 puan doğru cevap için
        }

        QuizAnswer answer = QuizAnswer.builder()
                .session(session)
                .question(question)
                .userEmail(email)
                .selectedOptionIndex(request.selectedOptionIndex())
                .answeredInMs(request.answeredInMs())
                .score(score)
                .correct(isCorrect)
                .build();
        answerRepository.save(answer);

        // Katılımcı skorunu güncelle
        participant.setTotalScore(participant.getTotalScore() + score);
        participant.setAnsweredCount(participant.getAnsweredCount() + 1);
        if (isCorrect) {
            participant.setCorrectCount(participant.getCorrectCount() + 1);
        }
        participantRepository.save(participant);

        // Cevap sayısını broadcast et
        broadcastAnswerCount(session.getTeam().getId(), sessionId, questionId);

        // Doğru cevap ve puan bilgisi henüz gizli — host "Cevapları Göster" diyene kadar
        return QuizAnswerResponse.hidden(answer);
    }

    /**
     * Soru sonucunu göster (host tarafından soru sonunda çağrılır).
     * Doğru cevap + leaderboard ile birlikte tüm state broadcast edilir.
     * resultsRevealed=true olarak gönderilir.
     */
    @Transactional(readOnly = true)
    public QuizSessionResponse showQuestionResult(UUID sessionId) {
        String email = currentEmail();
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getHostEmail().equals(email)) {
            throw new RuntimeException("Sadece host sonuçları açabilir");
        }

        QuizSessionResponse response = buildSessionResponse(session, true);
        broadcastState(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Oturumu sonlandırır.
     */
    @Transactional
    public QuizSessionResponse finishSession(QuizSession session) {
        session.setStatus(QuizSessionStatus.FINISHED);
        session.setFinishedAt(LocalDateTime.now());
        sessionRepository.save(session);

        QuizSessionResponse response = buildSessionResponse(session);
        broadcastState(session.getTeam().getId(), response);
        return response;
    }

    @Transactional
    public QuizSessionResponse finishSession(UUID sessionId) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        return finishSession(session);
    }

    // ─── Rapor ──────────────────────────────────────────────────────────────────

    /**
     * Tamamlanmış oturum raporu: katılımcı başına detaylı cevap bilgileri.
     */
    public Map<String, Object> getSessionReport(UUID sessionId) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        List<QuizParticipantResponse> participants = participantRepository
                .findBySessionIdOrderByTotalScoreDesc(sessionId)
                .stream().map(QuizParticipantResponse::from).toList();

        List<QuizAnswerResponse> answers = answerRepository.findBySessionId(sessionId)
                .stream().map(QuizAnswerResponse::from).toList();

        List<QuizQuestionResponse> questions = session.getTemplate().getQuestions()
                .stream().map(QuizQuestionResponse::from).toList();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("session", buildSessionResponse(session));
        report.put("participants", participants);
        report.put("answers", answers);
        report.put("questions", questions);
        return report;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private QuizSessionResponse buildSessionResponse(QuizSession session) {
        return buildSessionResponse(session, false);
    }

    private QuizSessionResponse buildSessionResponse(QuizSession session, boolean resultsRevealed) {
        List<QuizParticipantResponse> participants = participantRepository
                .findBySessionIdOrderByTotalScoreDesc(session.getId())
                .stream().map(QuizParticipantResponse::from).toList();

        // Leaderboard
        AtomicInteger rank = new AtomicInteger(1);
        List<QuizLeaderboardEntry> leaderboard = participants.stream()
                .map(p -> new QuizLeaderboardEntry(
                        rank.getAndIncrement(),
                        p.email(),
                        p.displayName(),
                        p.totalScore(),
                        p.correctCount(),
                        p.answeredCount()
                )).toList();

        // Aktif soru
        QuizQuestionResponse currentQ = null;
        int correctOptionIndex = -1;
        int idx = session.getCurrentQuestionIndex();
        if (idx >= 0 && idx < session.getTemplate().getQuestions().size()) {
            QuizQuestion question = session.getTemplate().getQuestions().get(idx);
            currentQ = QuizQuestionResponse.forPlayer(question);
            if (resultsRevealed) {
                correctOptionIndex = question.getCorrectOptionIndex();
            }
        }

        return QuizSessionResponse.from(session, currentQ, participants, leaderboard,
                resultsRevealed, correctOptionIndex);
    }

    private void broadcastState(UUID teamId, QuizSessionResponse state) {
        String topic = "/topic/quiz/" + teamId + "/state";
        try {
            messagingTemplate.convertAndSend(topic, state);
        } catch (Exception e) {
            log.warn("[WS] Quiz state broadcast başarısız: {}", e.getMessage());
        }
    }

    private void broadcastAnswerCount(UUID teamId, UUID sessionId, UUID questionId) {
        String topic = "/topic/quiz/" + teamId + "/answered";
        long count = answerRepository.findBySessionIdAndQuestionId(sessionId, questionId).size();
        long total = participantRepository.findBySessionIdOrderByTotalScoreDesc(sessionId).size();
        try {
            messagingTemplate.convertAndSend(topic, Map.of(
                    "questionId", questionId.toString(),
                    "answeredCount", count,
                    "totalParticipants", total
            ));
        } catch (Exception e) {
            log.warn("[WS] Quiz answered broadcast başarısız: {}", e.getMessage());
        }
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}


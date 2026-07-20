package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Adam Asmaca takım oyunu motoru.
 *
 * Kurallar (kullanıcı isteğiyle belirlendi):
 *   • Ortak kelime herkese gösterilir, sırası gelen oyuncu tahmin eder.
 *   • Doğru harf  → +10, sıra AYNI oyuncuda kalır (seri yapabilir).
 *   • Yanlış harf → -5, adam bir parça asılır, sıra sonraki oyuncuya geçer.
 *   • Kelime tahmini doğru  → +50 bonus, tur biter.
 *   • Kelime tahmini yanlış → adam ASILMAZ, sadece sıra sonraki oyuncuya geçer.
 *   • Kelimeyi son harfiyle tamamlayan da +50 bonus alır.
 *   • Moderatör kelimeleri kendi belirlediyse oynayamaz (izleyici olur).
 *   • Oyun sürerken katılan oyuncu sıranın sonuna eklenir.
 *
 * WebSocket stratejisi: Data-Carrying
 *   /topic/hangman/{teamId}/state → oturumun tam durumu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HangmanSessionService {

    /** Adamın tamamlanması için gereken yanlış harf sayısı. */
    public static final int MAX_WRONG = 6;

    static final int SCORE_CORRECT_LETTER = 10;
    static final int SCORE_WRONG_LETTER = -5;
    static final int SCORE_WORD_BONUS = 50;

    /** Canlı akışta gösterilecek tahmin sayısı. */
    private static final int RECENT_GUESS_LIMIT = 12;

    private static final int DEFAULT_ROUND_COUNT = 5;

    private static final Pattern TR_WORD = Pattern.compile("^[a-zçğıöşü]{2,30}$");
    private static final Pattern EN_WORD = Pattern.compile("^[a-z]{2,30}$");
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr");

    private final HangmanSessionRepository sessionRepository;
    private final HangmanRoundRepository roundRepository;
    private final HangmanParticipantRepository participantRepository;
    private final HangmanGuessRepository guessRepository;
    private final HangmanWordRepository wordRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EntitlementService entitlementService;

    // ─── Oturum Yönetimi ────────────────────────────────────────────────────────

    /**
     * Yeni oturum açar (LOBBY). Kelimeler burada seçilir ve turlar önceden oluşturulur.
     */
    @Transactional
    public HangmanSessionResponse startSession(UUID teamId, HangmanSessionStartRequest request) {
        String email = currentEmail();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Takım bulunamadı"));
        entitlementService.assertFeature(team.getOrganization(), PlanFeature.QUIZ);

        sessionRepository.findByTeamIdAndStatus(teamId, HangmanSessionStatus.LOBBY)
                .ifPresent(s -> { throw new RuntimeException("Bu takımda zaten bekleyen bir adam asmaca oturumu var"); });
        sessionRepository.findByTeamIdAndStatus(teamId, HangmanSessionStatus.IN_PROGRESS)
                .ifPresent(s -> { throw new RuntimeException("Bu takımda zaten devam eden bir adam asmaca oyunu var"); });

        String language = normalizeLanguage(request.language());
        List<String> customWords = normalizeCustomWords(request.customWords(), language);
        boolean custom = !customWords.isEmpty();

        // Kelimeleri moderatör belirlediyse cevapları bildiği için oynayamaz.
        boolean moderatorPlays = !custom && !Boolean.FALSE.equals(request.moderatorPlays());

        List<String> words = custom
                ? customWords
                : drawRandomWords(language, request.roundCount() == null ? DEFAULT_ROUND_COUNT : request.roundCount());

        if (words.isEmpty()) {
            throw new RuntimeException("Oyun için kelime bulunamadı");
        }

        String displayName = displayName(teamId, email);

        HangmanSession session = HangmanSession.builder()
                .team(team)
                .hostEmail(email)
                .hostName(displayName)
                .language(language)
                .wordSource(custom ? HangmanWordSource.CUSTOM : HangmanWordSource.RANDOM)
                .moderatorPlays(moderatorPlays)
                .build();
        sessionRepository.save(session);

        for (int i = 0; i < words.size(); i++) {
            roundRepository.save(HangmanRound.builder()
                    .session(session)
                    .roundOrder(i)
                    .word(words.get(i))
                    .build());
        }

        // Moderatörü katılımcı olarak ekle; oynamıyorsa izleyici (sıraya girmez, sıralamada yok).
        participantRepository.save(HangmanParticipant.builder()
                .session(session)
                .userEmail(email)
                .displayName(displayName)
                .spectator(!moderatorPlays)
                .turnOrder(0)
                .build());

        HangmanSessionResponse response = buildResponse(reload(session.getId()));
        broadcast(teamId, response);
        return response;
    }

    /**
     * Oturuma katılır. Oyun sürerken katılan oyuncu sıranın sonuna eklenir.
     */
    @Transactional
    public HangmanSessionResponse joinSession(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);

        if (session.getStatus() == HangmanSessionStatus.FINISHED) {
            throw new RuntimeException("Bu oyun zaten tamamlanmış");
        }

        if (!participantRepository.existsBySessionIdAndUserEmail(sessionId, email)) {
            List<HangmanParticipant> existing = participantRepository.findBySessionIdOrderByTurnOrderAsc(sessionId);
            int nextOrder = existing.stream()
                    .mapToInt(HangmanParticipant::getTurnOrder)
                    .max().orElse(-1) + 1;

            participantRepository.save(HangmanParticipant.builder()
                    .session(session)
                    .userEmail(email)
                    .displayName(displayName(session.getTeam().getId(), email))
                    .turnOrder(nextOrder)
                    .spectator(false)
                    .build());

            // Oyun tek kişiyle başlamış ve sıra boştaysa yeni gelen oynayabilsin.
            if (session.getStatus() == HangmanSessionStatus.IN_PROGRESS && session.getCurrentTurnEmail() == null) {
                session.setCurrentTurnEmail(email);
                sessionRepository.save(session);
            }
        }

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Oyunu başlatır — ilk turu açar ve sırayı ilk oyuncuya verir. Sadece moderatör.
     */
    @Transactional
    public HangmanSessionResponse beginGame(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertHost(session, email);

        if (session.getStatus() != HangmanSessionStatus.LOBBY) {
            throw new RuntimeException("Oyun zaten başlamış");
        }

        List<HangmanParticipant> players = players(sessionId);
        if (players.isEmpty()) {
            throw new RuntimeException("Oyuna en az bir oyuncu katılmalı");
        }

        HangmanRound first = roundRepository.findBySessionIdAndRoundOrder(sessionId, 0)
                .orElseThrow(() -> new RuntimeException("Tur bulunamadı"));
        first.setStatus(HangmanRoundStatus.ACTIVE);
        first.setStartedAt(LocalDateTime.now());
        roundRepository.save(first);

        session.setStatus(HangmanSessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        session.setCurrentRoundIndex(0);
        session.setCurrentTurnEmail(players.get(0).getUserEmail());
        sessionRepository.save(session);

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    // ─── Oyun Akışı ─────────────────────────────────────────────────────────────

    /**
     * Harf tahmini. Doğruysa sıra oyuncuda kalır; yanlışsa adam asılır ve sıra devreder.
     */
    @Transactional
    public HangmanSessionResponse guessLetter(UUID sessionId, String rawLetter) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        HangmanRound round = activeRound(session);
        HangmanParticipant player = assertTurn(session, email);

        String letter = normalizeLetter(rawLetter, session.getLanguage());
        if (round.getGuessedLetters().contains(letter)) {
            throw new RuntimeException("Bu harf zaten denendi");
        }

        boolean hit = round.getWord().contains(letter);
        round.getGuessedLetters().add(letter);

        int delta;
        if (hit) {
            delta = SCORE_CORRECT_LETTER;
            player.setCorrectLetterCount(player.getCorrectLetterCount() + 1);
        } else {
            delta = SCORE_WRONG_LETTER;
            player.setWrongLetterCount(player.getWrongLetterCount() + 1);
            round.setWrongCount(round.getWrongCount() + 1);
        }
        player.setTotalScore(player.getTotalScore() + delta);

        boolean solved = isFullyRevealed(round);
        if (solved) {
            // Kelimeyi son harfiyle tamamlayan da bonusu alır.
            delta += SCORE_WORD_BONUS;
            player.setTotalScore(player.getTotalScore() + SCORE_WORD_BONUS);
            player.setWordsSolved(player.getWordsSolved() + 1);
        }

        participantRepository.save(player);
        roundRepository.save(round);
        recordGuess(session, round, player, HangmanGuessType.LETTER, letter, hit, delta);

        if (solved) {
            closeRound(session, round, HangmanRoundStatus.SOLVED, player);
        } else if (round.getWrongCount() >= MAX_WRONG) {
            closeRound(session, round, HangmanRoundStatus.FAILED, null);
        } else if (!hit) {
            // Yanlış harfte sıra devreder; doğru harfte oyuncu devam eder.
            advanceTurn(session, email);
        }

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Kelimenin tamamını tahmin eder. Yanlışsa adam ASILMAZ, sadece sıra devreder.
     */
    @Transactional
    public HangmanSessionResponse guessWord(UUID sessionId, String rawWord) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        HangmanRound round = activeRound(session);
        HangmanParticipant player = assertTurn(session, email);

        String guess = rawWord == null ? "" : rawWord.trim().toLowerCase(localeOf(session.getLanguage()));
        if (guess.isEmpty()) {
            throw new RuntimeException("Tahmin boş olamaz");
        }

        boolean correct = round.getWord().equals(guess);
        int delta = 0;

        if (correct) {
            delta = SCORE_WORD_BONUS;
            player.setTotalScore(player.getTotalScore() + delta);
            player.setWordsSolved(player.getWordsSolved() + 1);
            participantRepository.save(player);

            // Kelime bulundu → tüm harfleri açılmış say.
            revealAll(round);
            roundRepository.save(round);
        }

        recordGuess(session, round, player, HangmanGuessType.WORD, guess, correct, delta);

        if (correct) {
            closeRound(session, round, HangmanRoundStatus.SOLVED, player);
        } else {
            // Ceza yok, adam asılmaz — kaybedilen tek şey sıra.
            advanceTurn(session, email);
        }

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Sırayı zorla devreder (oyuncu bağlantıyı kopardığında moderatör kullanır).
     */
    @Transactional
    public HangmanSessionResponse skipTurn(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertHost(session, email);

        if (session.getStatus() != HangmanSessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Oyun aktif değil");
        }
        advanceTurn(session, session.getCurrentTurnEmail());

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Oyunu erken bitirir. Sadece moderatör.
     */
    @Transactional
    public HangmanSessionResponse finishSession(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertHost(session, email);
        return doFinish(session);
    }

    // ─── Sorgular ───────────────────────────────────────────────────────────────

    public HangmanSessionResponse getActiveSession(UUID teamId) {
        HangmanSession session = sessionRepository.findByTeamIdAndStatus(teamId, HangmanSessionStatus.LOBBY)
                .or(() -> sessionRepository.findByTeamIdAndStatus(teamId, HangmanSessionStatus.IN_PROGRESS))
                .orElse(null);
        return session == null ? null : buildResponse(session);
    }

    public HangmanSessionResponse getSession(UUID sessionId) {
        return buildResponse(findSession(sessionId));
    }

    public List<HangmanSessionResponse> getHistory(UUID teamId) {
        return sessionRepository.findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, HangmanSessionStatus.FINISHED)
                .stream().map(this::buildResponse).toList();
    }

    // ─── Tur / sıra mantığı ─────────────────────────────────────────────────────

    /**
     * Turu kapatır ve sonraki tura geçer; tur kalmadıysa oyunu bitirir.
     * Sıra, turu bitiren oyuncunun bir sonrasına geçer.
     */
    private void closeRound(HangmanSession session, HangmanRound round,
                            HangmanRoundStatus status, HangmanParticipant solver) {
        round.setStatus(status);
        round.setFinishedAt(LocalDateTime.now());
        if (solver != null) {
            round.setSolvedByEmail(solver.getUserEmail());
            round.setSolvedByName(solver.getDisplayName());
        }
        roundRepository.save(round);

        int nextIndex = round.getRoundOrder() + 1;
        Optional<HangmanRound> next = roundRepository.findBySessionIdAndRoundOrder(session.getId(), nextIndex);

        if (next.isEmpty()) {
            doFinish(session);
            return;
        }

        HangmanRound nextRound = next.get();
        nextRound.setStatus(HangmanRoundStatus.ACTIVE);
        nextRound.setStartedAt(LocalDateTime.now());
        roundRepository.save(nextRound);

        session.setCurrentRoundIndex(nextIndex);
        sessionRepository.save(session);

        // Yeni tur, turu bitiren oyuncunun sonrasındaki oyuncuyla başlar.
        advanceTurn(session, session.getCurrentTurnEmail());
    }

    /**
     * Sırayı {@code fromEmail}'den sonraki oyuncuya taşır (izleyiciler atlanır).
     */
    private void advanceTurn(HangmanSession session, String fromEmail) {
        List<HangmanParticipant> players = players(session.getId());
        if (players.isEmpty()) {
            session.setCurrentTurnEmail(null);
            sessionRepository.save(session);
            return;
        }

        int currentIdx = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUserEmail().equals(fromEmail)) {
                currentIdx = i;
                break;
            }
        }

        // Sıradaki oyuncu; mevcut oyuncu listede yoksa (ayrıldıysa) baştan başla.
        HangmanParticipant next = players.get((currentIdx + 1) % players.size());
        session.setCurrentTurnEmail(next.getUserEmail());
        sessionRepository.save(session);
    }

    private HangmanSessionResponse doFinish(HangmanSession session) {
        session.setStatus(HangmanSessionStatus.FINISHED);
        session.setFinishedAt(LocalDateTime.now());
        session.setCurrentTurnEmail(null);
        sessionRepository.save(session);

        HangmanSessionResponse response = buildResponse(reload(session.getId()));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /** Sıraya giren oyuncular (izleyiciler hariç), tur sırasına göre. */
    private List<HangmanParticipant> players(UUID sessionId) {
        return participantRepository.findBySessionIdOrderByTurnOrderAsc(sessionId)
                .stream().filter(p -> !Boolean.TRUE.equals(p.getSpectator())).toList();
    }

    private boolean isFullyRevealed(HangmanRound round) {
        for (char c : round.getWord().toCharArray()) {
            if (!round.getGuessedLetters().contains(String.valueOf(c))) return false;
        }
        return true;
    }

    private void revealAll(HangmanRound round) {
        for (char c : round.getWord().toCharArray()) {
            String ch = String.valueOf(c);
            if (!round.getGuessedLetters().contains(ch)) {
                round.getGuessedLetters().add(ch);
            }
        }
    }

    private void recordGuess(HangmanSession session, HangmanRound round, HangmanParticipant player,
                             HangmanGuessType type, String value, boolean correct, int scoreDelta) {
        guessRepository.save(HangmanGuess.builder()
                .session(session)
                .round(round)
                .userEmail(player.getUserEmail())
                .displayName(player.getDisplayName())
                .guessType(type)
                .value(value)
                .correct(correct)
                .scoreDelta(scoreDelta)
                .build());
    }

    // ─── Doğrulamalar ───────────────────────────────────────────────────────────

    private HangmanSession findSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Oturum bulunamadı"));
    }

    private HangmanSession reload(UUID sessionId) {
        return findSession(sessionId);
    }

    private void assertHost(HangmanSession session, String email) {
        if (!session.getHostEmail().equals(email)) {
            throw new RuntimeException("Bu işlemi sadece moderatör yapabilir");
        }
    }

    private HangmanRound activeRound(HangmanSession session) {
        if (session.getStatus() != HangmanSessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Oyun aktif değil");
        }
        return roundRepository.findBySessionIdAndRoundOrder(session.getId(), session.getCurrentRoundIndex())
                .filter(r -> r.getStatus() == HangmanRoundStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Aktif tur yok"));
    }

    /** Sıranın gerçekten bu oyuncuda olduğunu doğrular. */
    private HangmanParticipant assertTurn(HangmanSession session, String email) {
        HangmanParticipant player = participantRepository
                .findBySessionIdAndUserEmail(session.getId(), email)
                .orElseThrow(() -> new RuntimeException("Bu oyuna katılmadınız"));

        if (Boolean.TRUE.equals(player.getSpectator())) {
            throw new RuntimeException("Kelimeleri siz belirlediğiniz için bu oyunda oynayamazsınız");
        }
        if (!email.equals(session.getCurrentTurnEmail())) {
            throw new RuntimeException("Sıra sizde değil");
        }
        return player;
    }

    // ─── Kelime seçimi ──────────────────────────────────────────────────────────

    /**
     * DB havuzu + dahili havuzdan tekrarsız rastgele kelime çeker.
     */
    private List<String> drawRandomWords(String language, int count) {
        Set<String> pool = new LinkedHashSet<>(HangmanWordPool.forLanguage(language));
        wordRepository.findByLanguageOrderByCreatedAtDesc(language)
                .forEach(w -> pool.add(w.getWord()));

        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(Math.max(1, count)).toList();
    }

    private List<String> normalizeCustomWords(List<String> words, String language) {
        if (words == null || words.isEmpty()) return List.of();

        Locale locale = localeOf(language);
        Pattern pattern = "tr".equals(language) ? TR_WORD : EN_WORD;

        List<String> result = new ArrayList<>();
        for (String raw : words) {
            if (raw == null) continue;
            String normalized = raw.trim().toLowerCase(locale);
            if (normalized.isEmpty()) continue;
            if (!pattern.matcher(normalized).matches()) {
                throw new IllegalArgumentException("Geçersiz kelime: " + raw.trim()
                        + " (boşluksuz, 2-30 harf ve seçilen dilin alfabesinde olmalı)");
            }
            if (!result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalizeLetter(String raw, String language) {
        String letter = raw == null ? "" : raw.trim().toLowerCase(localeOf(language));
        if (letter.length() != 1) {
            throw new IllegalArgumentException("Tek bir harf gönderin");
        }
        Pattern pattern = "tr".equals(language) ? TR_WORD : EN_WORD;
        // Tek harfi de aynı alfabeyle doğrula (desen en az 2 harf istediği için ikile).
        if (!pattern.matcher(letter.repeat(2)).matches()) {
            throw new IllegalArgumentException("Geçersiz harf: " + raw);
        }
        return letter;
    }

    private String normalizeLanguage(String language) {
        String lang = language == null ? "" : language.trim().toLowerCase(Locale.ENGLISH);
        if (!lang.equals("tr") && !lang.equals("en")) {
            throw new IllegalArgumentException("Desteklenmeyen dil: " + language);
        }
        return lang;
    }

    private Locale localeOf(String language) {
        return "tr".equals(language) ? TR_LOCALE : Locale.ENGLISH;
    }

    // ─── Yanıt üretimi ──────────────────────────────────────────────────────────

    private HangmanSessionResponse buildResponse(HangmanSession session) {
        UUID sessionId = session.getId();

        List<HangmanParticipant> all = participantRepository.findBySessionIdOrderByTurnOrderAsc(sessionId);
        List<HangmanParticipantResponse> participants = all.stream()
                .map(HangmanParticipantResponse::from).toList();

        // Sıralama: izleyiciler yarışmaz.
        List<HangmanParticipant> ranked = new ArrayList<>(all.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getSpectator())).toList());
        ranked.sort(Comparator.comparingInt(HangmanParticipant::getTotalScore).reversed()
                .thenComparing(Comparator.comparingInt(HangmanParticipant::getWordsSolved).reversed()));

        List<HangmanLeaderboardEntry> leaderboard = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            HangmanParticipant p = ranked.get(i);
            leaderboard.add(new HangmanLeaderboardEntry(
                    i + 1,
                    p.getUserEmail(),
                    p.getDisplayName(),
                    p.getTotalScore(),
                    p.getWordsSolved(),
                    p.getCorrectLetterCount(),
                    p.getWrongLetterCount()
            ));
        }

        List<HangmanRound> rounds = roundRepository.findBySessionIdOrderByRoundOrderAsc(sessionId);

        int idx = session.getCurrentRoundIndex();
        HangmanRoundResponse round = rounds.stream()
                .filter(r -> r.getRoundOrder() == idx)
                .findFirst()
                .map(r -> HangmanRoundResponse.from(r, MAX_WRONG))
                .orElse(null);

        // Biten son tur — istemci "kelime neydi" bilgisini bundan gösterir.
        HangmanRoundResponse lastFinished = rounds.stream()
                .filter(r -> r.getStatus() == HangmanRoundStatus.SOLVED
                        || r.getStatus() == HangmanRoundStatus.FAILED)
                .reduce((a, b) -> b)
                .map(r -> HangmanRoundResponse.from(r, MAX_WRONG))
                .orElse(null);

        String currentTurnName = all.stream()
                .filter(p -> p.getUserEmail().equals(session.getCurrentTurnEmail()))
                .map(HangmanParticipant::getDisplayName)
                .findFirst().orElse(null);

        List<HangmanGuessResponse> recentGuesses = guessRepository
                .findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream().limit(RECENT_GUESS_LIMIT)
                .map(HangmanGuessResponse::from).toList();

        return HangmanSessionResponse.from(session, round, lastFinished, currentTurnName, rounds.size(),
                participants, leaderboard, recentGuesses);
    }

    private void broadcast(UUID teamId, HangmanSessionResponse state) {
        try {
            messagingTemplate.convertAndSend("/topic/hangman/" + teamId + "/state", state);
        } catch (Exception e) {
            log.warn("[WS] Hangman state broadcast başarısız: {}", e.getMessage());
        }
    }

    private String displayName(UUID teamId, String email) {
        return teamMemberRepository.findByTeamIdAndEmail(teamId, email)
                .map(TeamMember::getDisplayName).orElse(email);
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
 *   • Kelime tahmini doğru  → kalan FARKLI harf sayısı × 10 puan, tur biter.
 *   • Kelime tahmini yanlış → adam ASILMAZ, sadece sıra sonraki oyuncuya geçer.
 *   • Moderatör kelimeleri kendi belirlediyse oynayamaz (izleyici olur).
 *   • Oyun sürerken katılan oyuncu sıranın sonuna eklenir.
 *   • Tur bitince oyun otomatik ilerlemez: kelime herkese ilan edilir ve moderatör
 *     "Sonraki Kelime"ye basana kadar ara ekranında beklenir (bkz. awaitingNextRound).
 *
 * Süre kuralları:
 *   • Harf tahmini HER ZAMAN yalnızca sırası gelen oyuncuya aittir.
 *   • Kelime tahmini sıranın ilk {@link #WORD_GUESS_LOCK_SECONDS} saniyesinde yalnızca sırası
 *     gelene açıktır; sonrasında diğer oyuncular da (izleyiciler hariç) deneyebilir.
 *   • Bir sıra {@link #TURN_SECONDS} saniye sürer; süre dolduğunda sıra kendiliğinden devreder.
 *   • Sırası gelen doğru harf bulup sırayı koruduğunda sayaç baştan başlar — dolayısıyla
 *     seri yapan oyuncuda kelime kilidi de yeniden kapanır (hak ettiği avantaj).
 *
 * Kelime tahmini kilidi açıldığında sırası olmayan oyuncu, o sıra penceresinde YALNIZCA BİR
 * kelime tahmini yapabilir. Bu sınır olmasa yanlış tahminin bedeli olmadığı için (sırası
 * olmayan oyuncu sırasını kaybetmez) kelimeyi deneme yanılmayla kırmak mümkün olurdu.
 *
 * Puanlama tasarımı — istismara kapalı:
 *   Her kelime sabit bir "puan havuzu" eder: (farklı harf sayısı) × 10. Kelimeyi
 *   tahmin eden, havuzun kalanını topluca alır. Bu yüzden "harfleri tek tek topla,
 *   en sonda kelimeyi tahmin et" taktiği ekstra puan KAZANDIRMAZ — erken tahmin
 *   etmekle aynı toplamı verir. Ayrı bir kelime bonusu bilinçli olarak yoktur;
 *   bonus eklenirse istismar geri gelir.
 *
 * Değişmez (invariant): totalScore == correctLetterCount × 10 - wrongLetterCount × 5
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

    /** Bir sıranın süresi. Dolduğunda tahmin gelmemişse sıra sonraki oyuncuya geçer. */
    public static final int TURN_SECONDS = 30;

    /** Sıranın ilk bu kadar saniyesinde kelime tahmini yalnızca sırası gelene açıktır. */
    public static final int WORD_GUESS_LOCK_SECONDS = 10;

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

        HangmanCategory requestedCategory = HangmanCategory.parse(request.category()).orElse(null);

        // Rastgele oyunda kategori havuzu daraltır ve lobide herkese görünür; null = karışık.
        // Moderatörün kendi kelimelerinde ise kategori yalnızca bir ipucudur: oturuma değil
        // turlara yazılır ve oyun sırasında moderatör açana kadar kimseye gösterilmez.
        HangmanCategory sessionCategory = custom ? null : requestedCategory;

        // Kelimeleri moderatör belirlediyse cevapları bildiği için oynayamaz.
        boolean moderatorPlays = !custom && !Boolean.FALSE.equals(request.moderatorPlays());

        List<DrawnWord> words = custom
                ? customWords.stream().map(w -> new DrawnWord(w, requestedCategory)).toList()
                : drawRandomWords(language, request.roundCount() == null ? DEFAULT_ROUND_COUNT : request.roundCount(),
                        sessionCategory);

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
                .category(sessionCategory)
                .moderatorPlays(moderatorPlays)
                .build();
        sessionRepository.save(session);

        // Sabit kategorili oyunda kategori zaten lobide yazıyor; ipucu olarak saklamanın anlamı yok.
        boolean categoryAlreadyPublic = sessionCategory != null;

        for (int i = 0; i < words.size(); i++) {
            DrawnWord drawn = words.get(i);
            roundRepository.save(HangmanRound.builder()
                    .session(session)
                    .roundOrder(i)
                    .word(drawn.word())
                    .category(drawn.category())
                    .categoryRevealed(categoryAlreadyPublic && drawn.category() != null)
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
        if (session.getStatus() == HangmanSessionStatus.CANCELLED) {
            throw new RuntimeException("Bu lobi kapatıldı");
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
                session.setTurnStartedAt(LocalDateTime.now());
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
        session.setTurnStartedAt(LocalDateTime.now());
        sessionRepository.save(session);

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    // ─── Oyun Akışı ─────────────────────────────────────────────────────────────

    /**
     * Harf tahmini — yalnızca sırası gelen oyuncu yapabilir.
     * Doğruysa sıra oyuncuda kalır (süre baştan başlar); yanlışsa adam asılır ve sıra devreder.
     */
    @Transactional
    public HangmanSessionResponse guessLetter(UUID sessionId, String rawLetter) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);
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
            // Ayrıca bonus YOK: son harf de diğer harfler kadar değer.
            // Aksi hâlde harfleri tek tek toplayıp sonunda bitirmek ekstra kazandırırdı.
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
        } else {
            // Sıra oyuncuda kaldı: hamle yaptığı için 30 sn'lik süresi baştan başlar.
            session.setTurnStartedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Kelimenin tamamını tahmin eder.
     *
     * Sıranın ilk {@link #WORD_GUESS_LOCK_SECONDS} saniyesi sırası gelen oyuncuya ayrılmıştır;
     * o pencere kapandıktan sonra diğer oyuncular da yarışa girer (herkes o sıra penceresinde
     * bir kez deneyebilir). Yanlış tahminde adam ASILMAZ:
     *   • sırası gelen tahmin ettiyse sırasını kaybeder,
     *   • sırası olmayan tahmin ettiyse sıra bozulmaz, sadece o sıradaki hakkı biter.
     */
    @Transactional
    public HangmanSessionResponse guessWord(UUID sessionId, String rawWord) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);
        boolean myTurn = email.equals(session.getCurrentTurnEmail());
        HangmanParticipant player = assertCanGuessWord(session, round, email, myTurn);

        String guess = rawWord == null ? "" : rawWord.trim().toLowerCase(localeOf(session.getLanguage()));
        if (guess.isEmpty()) {
            throw new RuntimeException("Tahmin boş olamaz");
        }

        boolean correct = round.getWord().equals(guess);
        int delta = 0;

        if (correct) {
            // Kelimeyi bilen, kalan harflerin TAMAMINI bilmiş sayılır.
            // Böylece "harfleri tek tek topla, en sonda tahmin et" istismarı ortadan kalkar:
            // kelime her hâlükârda aynı toplam puanı eder, kim açarsa o alır.
            int remaining = countUnrevealedLetters(round);
            delta = remaining * SCORE_CORRECT_LETTER;

            player.setTotalScore(player.getTotalScore() + delta);
            player.setCorrectLetterCount(player.getCorrectLetterCount() + remaining);
            player.setWordsSolved(player.getWordsSolved() + 1);
            participantRepository.save(player);

            // Kelime bulundu → tüm harfleri açılmış say.
            revealAll(round);
            roundRepository.save(round);
        }

        recordGuess(session, round, player, HangmanGuessType.WORD, guess, correct, delta);

        if (correct) {
            closeRound(session, round, HangmanRoundStatus.SOLVED, player);
        } else if (myTurn) {
            // Ceza yok, adam asılmaz — kaybedilen tek şey sıra.
            advanceTurn(session, email);
        }
        // Sırası olmayanın yanlış tahmini sırayı bozmaz: sayaç kaldığı yerden işler,
        // o oyuncunun bu sıra penceresindeki tek hakkı harcanmış olur.

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
        if (Boolean.TRUE.equals(session.getAwaitingNextRound())) {
            throw new RuntimeException("Tur bitti — önce sonraki kelimeye geçin");
        }
        advanceTurn(session, session.getCurrentTurnEmail());

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Oynanan kelimenin kategorisini herkese açar (ipucu). Sadece moderatör.
     *
     * Kategori aynı anda TÜM oyunculara görünür olduğu için moderatör oyuncuysa da
     * (rastgele kelimelerde oynayabilir) kimseye avantaj sağlamaz. Geri alınamaz:
     * açılan kategori turun sonuna kadar açık kalır.
     */
    @Transactional
    public HangmanSessionResponse revealCategory(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertHost(session, email);

        HangmanRound round = activeRound(session);
        if (round.getCategory() == null) {
            throw new RuntimeException("Bu kelimenin kategorisi bilinmiyor");
        }

        round.setCategoryRevealed(true);
        roundRepository.save(round);

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Başlamamış lobiyi kapatır — kimse katılmadığı için vazgeçen moderatör içindir.
     *
     * Bitirmekten (FINISHED) ayrı bir durum: hiç oynanmamış oyun geçmişte boş bir kayıt
     * olarak görünmemeli. Kapatılan oturum "aktif oturum" sorgusuna da düşmediği için
     * moderatör hemen yeni bir oyun açabilir.
     */
    @Transactional
    public HangmanSessionResponse cancelSession(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertHost(session, email);

        if (session.getStatus() != HangmanSessionStatus.LOBBY) {
            throw new RuntimeException("Yalnızca başlamamış bir lobi kapatılabilir");
        }

        session.setStatus(HangmanSessionStatus.CANCELLED);
        session.setFinishedAt(LocalDateTime.now());
        session.setCurrentTurnEmail(null);
        session.setTurnStartedAt(null);
        sessionRepository.save(session);

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
     * Turu kapatır ve oyunu ara ekranına alır: kelime herkese açılır, sonraki tura
     * geçmek moderatörün {@link #nextRound(UUID)} çağrısını bekler.
     *
     * Otomatik geçilmemesinin sebebi: tur bitince cevabın ekranda birkaç saniye
     * yanıp sönmesi yerine masadaki herkesin "kelime neymiş" diye görüp konuşabilmesi.
     * Ara boyunca {@code currentRoundIndex} biten turda kalır (yanıt cevabı açık taşısın),
     * {@code currentTurnEmail} de turu bitiren oyuncuda durur — sonraki tur onun
     * sonrasındaki oyuncuyla başlasın diye.
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

        session.setAwaitingNextRound(true);
        sessionRepository.save(session);
    }

    /**
     * Ara ekranından sonraki tura geçer; tur kalmadıysa oyunu bitirir. Sadece moderatör.
     */
    @Transactional
    public HangmanSessionResponse nextRound(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertHost(session, email);

        if (session.getStatus() != HangmanSessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Oyun aktif değil");
        }
        if (!Boolean.TRUE.equals(session.getAwaitingNextRound())) {
            throw new RuntimeException("Oynanan tur henüz bitmedi");
        }

        int nextIndex = session.getCurrentRoundIndex() + 1;
        Optional<HangmanRound> next = roundRepository.findBySessionIdAndRoundOrder(session.getId(), nextIndex);

        if (next.isEmpty()) {
            // Son kelimeydi: ara ekranındaki buton oyunu bitirir.
            session.setAwaitingNextRound(false);
            return doFinish(session);
        }

        HangmanRound nextRound = next.get();
        nextRound.setStatus(HangmanRoundStatus.ACTIVE);
        nextRound.setStartedAt(LocalDateTime.now());
        roundRepository.save(nextRound);

        session.setCurrentRoundIndex(nextIndex);
        session.setAwaitingNextRound(false);
        sessionRepository.save(session);

        // Yeni tur, turu bitiren oyuncunun sonrasındaki oyuncuyla başlar.
        advanceTurn(session, session.getCurrentTurnEmail());

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Sırayı {@code fromEmail}'den sonraki oyuncuya taşır (izleyiciler atlanır).
     */
    private void advanceTurn(HangmanSession session, String fromEmail) {
        List<HangmanParticipant> players = players(session.getId());
        if (players.isEmpty()) {
            session.setCurrentTurnEmail(null);
            session.setTurnStartedAt(null);
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
        // Yeni sıra = yeni süre: 30 sn geri sayım ve 10 sn'lik kelime kilidi baştan başlar.
        session.setTurnStartedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    /**
     * Süresi dolan sıraları devreder — kimse tahmin etmediğinde oyunun takılmaması için.
     *
     * Devretmenin TEK yeri burasıdır: tahmin uçları süresi dolmuş sırayı yalnızca reddeder,
     * çünkü oradaki hata işlemi geri alıp devretmeyi de silerdi (bkz. assertTurnNotExpired).
     * Saniyede bir tarar; aktif adam asmaca oturumu genelde bir elin parmağını geçmez.
     */
    @Scheduled(fixedDelay = 1_000, initialDelay = 10_000)
    @Transactional
    public void sweepExpiredTurns() {
        for (HangmanSession session : sessionRepository.findByStatus(HangmanSessionStatus.IN_PROGRESS)) {
            try {
                if (session.getCurrentTurnEmail() == null) continue;

                // Sürüm öncesinde başlamış oturumlarda sayaç boştur — şimdiden başlat.
                if (session.getTurnStartedAt() == null) {
                    session.setTurnStartedAt(LocalDateTime.now());
                    sessionRepository.save(session);
                    continue;
                }
                if (!isTurnExpired(session)) continue;

                advanceTurn(session, session.getCurrentTurnEmail());
                broadcast(session.getTeam().getId(), buildResponse(reload(session.getId())));
            } catch (Exception e) {
                log.warn("[Hangman] Süre dolmuş sıra devredilemedi ({}): {}", session.getId(), e.getMessage());
            }
        }
    }

    private HangmanSessionResponse doFinish(HangmanSession session) {
        session.setStatus(HangmanSessionStatus.FINISHED);
        session.setFinishedAt(LocalDateTime.now());
        session.setCurrentTurnEmail(null);
        session.setTurnStartedAt(null);
        session.setAwaitingNextRound(false);
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

    /**
     * Henüz açılmamış FARKLI harf sayısı — kelime tahmininin puan değeri budur.
     *
     * Harf tahmininde bir harf, kelimede kaç kez geçerse geçsin tek seferde +10 kazandırır;
     * bu yüzden burada da tekrarlar değil farklı harfler sayılır. Böylece "kelimeyi bilmek"
     * ile "kalan harfleri tek tek bilmek" tam olarak aynı puanı eder.
     */
    private int countUnrevealedLetters(HangmanRound round) {
        Set<String> remaining = new LinkedHashSet<>();
        for (char c : round.getWord().toCharArray()) {
            String ch = String.valueOf(c);
            if (!round.getGuessedLetters().contains(ch)) {
                remaining.add(ch);
            }
        }
        return remaining.size();
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
        if (Boolean.TRUE.equals(session.getAwaitingNextRound())) {
            throw new RuntimeException("Tur bitti — moderatörün sonraki kelimeye geçmesi bekleniyor");
        }
        return roundRepository.findBySessionIdAndRoundOrder(session.getId(), session.getCurrentRoundIndex())
                .filter(r -> r.getStatus() == HangmanRoundStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Aktif tur yok"));
    }

    /**
     * Süresi dolmuş sırayla oynanmasını engeller.
     *
     * Sırayı burada DEVRETMEYİZ: bu metot her zaman hata fırlatan bir yolda çağrılır, hata da
     * işlemi geri alır — devretme kaydı da geri alınırdı. Devretme tek bir yerde,
     * {@link #sweepExpiredTurns()} zamanlayıcısında yapılır (saniyede bir tarar).
     */
    private void assertTurnNotExpired(HangmanSession session) {
        if (isTurnExpired(session)) {
            throw new RuntimeException("Süre doldu — sıra sonraki oyuncuya geçiyor");
        }
    }

    /**
     * Kelime tahmini hakkını doğrular.
     *
     * Sırası gelen her an tahmin edebilir. Diğer oyuncular yalnızca sıranın ilk
     * {@link #WORD_GUESS_LOCK_SECONDS} saniyesi dolduktan sonra ve o sıra penceresinde
     * bir kez deneyebilir. İzleyiciler (kelimeleri belirleyen moderatör) hiç oynayamaz.
     */
    private HangmanParticipant assertCanGuessWord(HangmanSession session, HangmanRound round,
                                                  String email, boolean myTurn) {
        HangmanParticipant player = participantRepository
                .findBySessionIdAndUserEmail(session.getId(), email)
                .orElseThrow(() -> new RuntimeException("Bu oyuna katılmadınız"));

        if (Boolean.TRUE.equals(player.getSpectator())) {
            throw new RuntimeException("Kelimeleri siz belirlediğiniz için bu oyunda oynayamazsınız");
        }
        if (myTurn) {
            return player;
        }

        int wait = wordOpenInSeconds(session);
        if (wait > 0) {
            throw new RuntimeException("Kelime tahmini ilk " + WORD_GUESS_LOCK_SECONDS
                    + " saniye sırası gelen oyuncunun hakkı — " + wait + " sn sonra sen de deneyebilirsin");
        }
        assertWordGuessQuotaLeft(session, round, email);
        return player;
    }

    /**
     * Sırası olmayan oyuncuya sıra başına tek kelime hakkı tanır.
     *
     * Yanlış tahminin bu oyunculara maliyeti yok (sıralarını kaybetmiyorlar); sınır olmasaydı
     * kilit açılır açılmaz kelimeyi deneme yanılmayla kırmak mümkün olurdu.
     */
    private void assertWordGuessQuotaLeft(HangmanSession session, HangmanRound round, String email) {
        LocalDateTime turnStart = session.getTurnStartedAt();
        if (turnStart == null) return;

        boolean alreadyTried = guessRepository.findByRoundIdOrderByCreatedAtAsc(round.getId()).stream()
                .anyMatch(g -> g.getGuessType() == HangmanGuessType.WORD
                        && email.equals(g.getUserEmail())
                        && g.getCreatedAt() != null
                        && !g.getCreatedAt().isBefore(turnStart));

        if (alreadyTried) {
            throw new RuntimeException("Bu sırada kelime hakkını kullandın — sıradaki oyuncuyu bekle");
        }
    }

    /** Aktif bir sıra var ve süresi doldu mu? turnStartedAt boşsa (eski oturum) süre işlemez. */
    private boolean isTurnExpired(HangmanSession session) {
        return session.getStatus() == HangmanSessionStatus.IN_PROGRESS
                && !Boolean.TRUE.equals(session.getAwaitingNextRound())
                && session.getCurrentTurnEmail() != null
                && session.getTurnStartedAt() != null
                && elapsedTurnSeconds(session) >= TURN_SECONDS;
    }

    /** Mevcut sıranın başlamasından bu yana geçen saniye. */
    private long elapsedTurnSeconds(HangmanSession session) {
        LocalDateTime start = session.getTurnStartedAt();
        if (start == null) return 0;
        return Math.max(0, Duration.between(start, LocalDateTime.now()).getSeconds());
    }

    /** Sıranın bitmesine kalan saniye; aktif sıra yoksa (ya da tur arasıysa) 0. */
    private int turnSecondsLeft(HangmanSession session) {
        if (session.getStatus() != HangmanSessionStatus.IN_PROGRESS || session.getCurrentTurnEmail() == null
                || Boolean.TRUE.equals(session.getAwaitingNextRound())) {
            return 0;
        }
        return (int) Math.max(0, TURN_SECONDS - elapsedTurnSeconds(session));
    }

    /** Kelime tahmininin herkese açılmasına kalan saniye; 0 = açık. */
    private int wordOpenInSeconds(HangmanSession session) {
        if (session.getStatus() != HangmanSessionStatus.IN_PROGRESS || session.getCurrentTurnEmail() == null
                || Boolean.TRUE.equals(session.getAwaitingNextRound())) {
            return 0;
        }
        return (int) Math.max(0, WORD_GUESS_LOCK_SECONDS - elapsedTurnSeconds(session));
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

    /** Bir tura yazılacak kelime ve geldiği kategori (kategori bilinmiyorsa null). */
    private record DrawnWord(String word, HangmanCategory category) {
    }

    /**
     * DB havuzu + dahili havuzdan tekrarsız rastgele kelime çeker.
     *
     * Kelimenin geldiği kategori de taşınır — moderatör oyun sırasında ipucu olarak
     * açabilsin diye tura yazılır (karışık oyunda her turun kategorisi farklı olabilir).
     *
     * @param category null ise tüm kategoriler karışık; doluysa sadece o kategori.
     *                 Kategori seçilmediğinde DB'deki kategorisiz (eski) kelimeler de havuza girer.
     */
    private List<DrawnWord> drawRandomWords(String language, int count, HangmanCategory category) {
        // Aynı kelime iki kategoride geçiyorsa ilk görülen kategori kalır (putIfAbsent).
        Map<String, HangmanCategory> pool = new LinkedHashMap<>();
        if (category == null) {
            for (HangmanCategory c : HangmanCategory.values()) {
                HangmanWordPool.forCategory(language, c).forEach(w -> pool.putIfAbsent(w, c));
            }
        } else {
            HangmanWordPool.forCategory(language, category).forEach(w -> pool.putIfAbsent(w, category));
        }

        List<HangmanWord> dbWords = category == null
                ? wordRepository.findByLanguageOrderByCreatedAtDesc(language)
                : wordRepository.findByLanguageAndCategoryOrderByCreatedAtDesc(language, category);
        dbWords.forEach(w -> pool.putIfAbsent(w.getWord(), w.getCategory()));

        List<DrawnWord> shuffled = new ArrayList<>(pool.entrySet().stream()
                .map(e -> new DrawnWord(e.getKey(), e.getValue()))
                .toList());
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
                .map(r -> HangmanRoundResponse.from(r, MAX_WRONG, session.getLanguage()))
                .orElse(null);

        // Biten son tur — istemci "kelime neydi" bilgisini bundan gösterir.
        HangmanRoundResponse lastFinished = rounds.stream()
                .filter(r -> r.getStatus() == HangmanRoundStatus.SOLVED
                        || r.getStatus() == HangmanRoundStatus.FAILED)
                .reduce((a, b) -> b)
                .map(r -> HangmanRoundResponse.from(r, MAX_WRONG, session.getLanguage()))
                .orElse(null);

        String currentTurnName = all.stream()
                .filter(p -> p.getUserEmail().equals(session.getCurrentTurnEmail()))
                .map(HangmanParticipant::getDisplayName)
                .findFirst().orElse(null);

        List<HangmanGuessResponse> recentGuesses = guessRepository
                .findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream().limit(RECENT_GUESS_LIMIT)
                .map(HangmanGuessResponse::from).toList();

        return HangmanSessionResponse.from(session, round, lastFinished, currentTurnName,
                TURN_SECONDS, WORD_GUESS_LOCK_SECONDS, turnSecondsLeft(session), wordOpenInSeconds(session),
                rounds.size(), participants, leaderboard, recentGuesses);
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

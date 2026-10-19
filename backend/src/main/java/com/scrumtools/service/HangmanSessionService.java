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
 * Üç mod vardır (bkz. {@link HangmanMode}); aşağıdaki kurallar sıralı (TURN) moda aittir,
 * yarış ve takım modunun farkları en altta.
 *
 * Kurallar (kullanıcı isteğiyle belirlendi):
 *   • Ortak kelime herkese gösterilir, sırası gelen oyuncu tahmin eder.
 *   • Doğru harf  → +10, sıra AYNI oyuncuda kalır (seri yapabilir).
 *   • Yanlış harf → -5, adam bir parça asılır, sıra sonraki oyuncuya geçer.
 *   • Kelime tahmini doğru  → kalan FARKLI harf sayısı × 10 puan, tur biter.
 *   • Kelime tahmini yanlış → adam ASILMAZ; sırası gelen tahmin ettiyse sıra sonraki
 *     oyuncuya geçer, sırası olmayan tahmin ettiyse {@link #SCORE_WRONG_WORD} puan kaybeder.
 *   • Moderatör kelimeleri kendi belirlediyse oynayamaz (izleyici olur).
 *   • Oyun sürerken katılan oyuncu sıranın sonuna eklenir.
 *   • Tur bitince kelime herkese ilan edilir (ara ekranı, bkz. awaitingNextRound). Otomatik
 *     geçiş açıksa ekran {@link #INTERMISSION_SECONDS} sn sonra kendiliğinden kapanır;
 *     kapalıysa ya da moderatör duraklattıysa "Sonraki Kelime"ye basılması beklenir.
 *
 * Süre kuralları (süreler oturumun temposundan gelir, bkz. {@link HangmanPace}):
 *   • Harf tahmini HER ZAMAN yalnızca sırası gelen oyuncuya aittir.
 *   • Kelime tahmini her an herkese (izleyiciler hariç) açıktır.
 *   • Bir sıra tempo kadar sürer; süre dolduğunda sıra kendiliğinden devreder.
 *   • Doğru harf sırayı korur ama sayacı SIFIRLAMAZ ve bir sırada en fazla {@link #MAX_STREAK}
 *     doğru harf bulunabilir — seri yapan oyuncu sırayı tekeline alıp diğerlerini bekletmesin.
 *
 * Tahmin oylaması: sırası olmayan oyuncular gizlice "sıradaki harf" tahmini bırakır.
 * Sırası gelen o harfi seçer ve harf kelimede varsa tahmin edenler {@link #SCORE_PREDICTION}
 * puan alır. Bekleme süresi boş geçmesin diye vardır; puanı bilerek küçüktür.
 *
 * Sırası olmayanın yanlış kelime tahmininin bedeli {@link #SCORE_WRONG_WORD} puandır. Eskiden
 * bunun yerine süreli bir kilit ve sıra başına tek hak vardı; bekleyen oyuncu çoğu zaman
 * hiçbir şey yapamıyordu. Ceza deneme yanılmayı aynı şekilde caydırır, ama oyuncu her an
 * oyunda kalır.
 *
 * Puanlama tasarımı — istismara kapalı:
 *   Her kelime sabit bir "puan havuzu" eder: (farklı harf sayısı) × 10. Kelimeyi
 *   tahmin eden, havuzun kalanını topluca alır. Bu yüzden "harfleri tek tek topla,
 *   en sonda kelimeyi tahmin et" taktiği ekstra puan KAZANDIRMAZ — erken tahmin
 *   etmekle aynı toplamı verir. Ayrı bir kelime bonusu bilinçli olarak yoktur;
 *   bonus eklenirse istismar geri gelir.
 *
 * Değişmez (invariant, sıralı modda):
 *   totalScore == correctLetterCount × 10 - wrongLetterCount × 5
 *                 + predictionHits × 2 - wrongWordCount × 10
 *
 * Yarış modu (RACE): herkes aynı kelimeyi kendi tahtasında ({@link HangmanRaceBoard}) aynı
 * anda çözer. Harf puanı yoktur; çözen bitiş sırasına göre puan alır (bkz. raceSolveScore),
 * yanlış kelime tahmini bir can götürür. Tur, herkes bitirince ya da yarış süresi dolunca kapanır.
 * Tahtalar gizlidir; herkese yalnızca ilerleme sayıları yayınlanır.
 *
 * Takım modu (TEAM): oyuncular iki takıma bölünür, sıra takımlar arasında geçer. Sırası gelen
 * takımın üyeleri harfe oy verir ({@link HangmanTeamVote}); herkes oy verince ya da süre
 * dolunca en çok oy alan harf oynanır. Puanlar hem takıma hem o harfe oy verenlere yazılır.
 * Kelime tahmini sıralı moddaki gibidir — "sırası gelen" burada sırası gelen takımın her üyesidir.
 * Takım oyları herkese açık yayınlandığı için bu modda tahmin oylaması yoktur.
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

    /** Tahmin oylamasında isabet başına puan — harf puanının yanında küçük kalsın diye düşük. */
    static final int SCORE_PREDICTION = 2;

    /** Bir sırada bulunabilecek en fazla doğru harf; sonra sıra sonraki oyuncuya geçer. */
    public static final int MAX_STREAK = 3;

    /** Sırası değilken yanlış kelime tahmininin bedeli. */
    static final int SCORE_WRONG_WORD = -10;

    /** Otomatik geçişte ara ekranının açık kalacağı süre. */
    public static final int INTERMISSION_SECONDS = 8;

    /** Yarışta kelimeyi ilk üç bitirenin puanı; sonrakiler {@link #RACE_MIN_SOLVE_SCORE} alır. */
    private static final int[] RACE_PLACE_SCORES = {50, 40, 30};
    private static final int RACE_MIN_SOLVE_SCORE = 20;
    /** Yarışta çözerken kalan her can için ek puan — hızın yanında dikkat de ödüllensin. */
    private static final int RACE_LIFE_BONUS = 5;

    /** Gönderilebilen tepkiler — serbest metin yok, akış emoji dışında bir şey taşımasın. */
    private static final Set<String> REACTION_EMOJIS = Set.of("👏", "🔥", "😂", "😱", "🤔", "💀", "🎉", "👀");
    /** Aynı oyuncunun iki tepkisi arasındaki en kısa süre — ekranı emoji yağmuruna boğmasın. */
    private static final long REACTION_MIN_INTERVAL_MS = 800;

    static final String[] TEAM_NAMES = {"🔵 Mavi", "🔴 Kırmızı"};

    /** Canlı akışta gösterilecek tahmin sayısı. */
    private static final int RECENT_GUESS_LIMIT = 12;

    private static final int DEFAULT_ROUND_COUNT = 5;

    private static final Pattern TR_WORD = Pattern.compile("^[a-zçğıöşü]{2,30}$");
    private static final Pattern EN_WORD = Pattern.compile("^[a-z]{2,30}$");
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr");

    /** "sessionId|email" → son tepki zamanı (ms). Bellekte; yeniden başlatmada sıfırlanması sorun değil. */
    private final Map<String, Long> lastReactionAt = new java.util.concurrent.ConcurrentHashMap<>();

    private final HangmanSessionRepository sessionRepository;
    private final HangmanRoundRepository roundRepository;
    private final HangmanParticipantRepository participantRepository;
    private final HangmanGuessRepository guessRepository;
    private final HangmanPredictionRepository predictionRepository;
    private final HangmanRaceBoardRepository raceBoardRepository;
    private final HangmanTeamVoteRepository teamVoteRepository;
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
                .mode(HangmanMode.parse(request.mode()))
                .pace(HangmanPace.parse(request.pace()))
                .autoAdvance(!Boolean.FALSE.equals(request.autoAdvance()))
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

            boolean inProgress = session.getStatus() == HangmanSessionStatus.IN_PROGRESS;
            HangmanMode mode = mode(session);

            participantRepository.save(HangmanParticipant.builder()
                    .session(session)
                    .userEmail(email)
                    .displayName(displayName(session.getTeam().getId(), email))
                    .turnOrder(nextOrder)
                    .spectator(false)
                    // Lobide takımlar oyun başlarken dağıtılır; oyun sürerken gelen küçük takıma girer.
                    .teamNo(inProgress && mode == HangmanMode.TEAM ? smallerTeam(sessionId) : null)
                    .build());

            // Sıralı oyun tek kişiyle başlamış ve sıra boştaysa yeni gelen oynayabilsin.
            if (inProgress && mode == HangmanMode.TURN && session.getCurrentTurnEmail() == null) {
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
        if (mode(session) == HangmanMode.TEAM) {
            if (players.size() < 2) {
                throw new RuntimeException("Takım modu için en az iki oyuncu gerekli");
            }
            // Katılım sırasıyla dönüşümlü dağıt: takımlar en fazla bir kişi farkla eşit olur.
            for (int i = 0; i < players.size(); i++) {
                players.get(i).setTeamNo(i % 2 + 1);
                participantRepository.save(players.get(i));
            }
        }

        HangmanRound first = roundRepository.findBySessionIdAndRoundOrder(sessionId, 0)
                .orElseThrow(() -> new RuntimeException("Tur bulunamadı"));
        first.setStatus(HangmanRoundStatus.ACTIVE);
        first.setStartedAt(LocalDateTime.now());
        roundRepository.save(first);

        session.setStatus(HangmanSessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        session.setCurrentRoundIndex(0);
        sessionRepository.save(session);
        startRoundTurn(session);

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    // ─── Oyun Akışı ─────────────────────────────────────────────────────────────

    /**
     * Harf tahmini — yalnızca sırası gelen oyuncu yapabilir.
     * Doğruysa sıra oyuncuda kalır (süre işlemeye devam eder, en fazla {@link #MAX_STREAK} harf);
     * yanlışsa adam asılır ve sıra devreder. Bekleyen tahmin oylamaları burada sonuçlanır.
     */
    @Transactional
    public HangmanSessionResponse guessLetter(UUID sessionId, String rawLetter) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertMode(session, HangmanMode.TURN);
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);
        HangmanParticipant player = assertTurn(session, email);

        String letter = normalizeLetter(rawLetter, session.getLanguage());
        if (round.getGuessedLetters().contains(letter)) {
            throw new RuntimeException("Bu harf zaten denendi");
        }

        boolean hit = round.getWord().contains(letter);
        // Seri sınırı için bu sırada daha önce bulunan doğru harfler (bu tahmin hariç).
        int streakBefore = hit ? lettersThisTurn(session, round, email) : 0;
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
        List<String> predictionWinners = resolvePredictions(session, letter, hit);
        recordGuess(session, round, player, HangmanGuessType.LETTER, letter, hit, delta, predictionWinners);

        if (solved) {
            closeRound(session, round, HangmanRoundStatus.SOLVED, player.getUserEmail(), player.getDisplayName());
        } else if (round.getWrongCount() >= MAX_WRONG) {
            closeRound(session, round, HangmanRoundStatus.FAILED, null, null);
        } else if (!hit || streakBefore + 1 >= MAX_STREAK) {
            // Yanlış harfte ya da seri sınırına gelindiğinde sıra devreder.
            advanceTurn(session, email);
        }
        // Aksi hâlde sıra oyuncuda kalır; süresi sıfırlanmaz, kaldığı yerden işler.

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Kelimenin tamamını tahmin eder (sıralı ve takım modu; yarışta bkz. raceGuessWord).
     *
     * Herkes her an deneyebilir. Yanlış tahminde adam ASILMAZ:
     *   • sırası gelen (takım modunda sırası gelen takımın üyesi) tahmin ettiyse sıra devreder,
     *   • sırası olmayan tahmin ettiyse sıra bozulmaz ama {@link #SCORE_WRONG_WORD} puan kaybeder
     *     (takım modunda takımı da).
     */
    @Transactional
    public HangmanSessionResponse guessWord(UUID sessionId, String rawWord) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        if (mode(session) == HangmanMode.RACE) {
            throw new RuntimeException("Yarış modunda kendi tahtanda tahmin et");
        }
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);
        HangmanParticipant player = assertPlayer(session, email);
        boolean myTurn = mode(session) == HangmanMode.TEAM
                ? Objects.equals(player.getTeamNo(), session.getCurrentTeam())
                : email.equals(session.getCurrentTurnEmail());

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
            addTeamScore(session, player.getTeamNo(), delta);

            // Kelime bulundu → tüm harfleri açılmış say.
            revealAll(round);
            roundRepository.save(round);
        } else if (!myTurn) {
            // Sırası olmayan için yanlış tahminin bedeli puan — yoksa deneme yanılmayla kırılırdı.
            delta = SCORE_WRONG_WORD;
            player.setTotalScore(player.getTotalScore() + delta);
            player.setWrongWordCount((player.getWrongWordCount() == null ? 0 : player.getWrongWordCount()) + 1);
            participantRepository.save(player);
            addTeamScore(session, player.getTeamNo(), delta);
        }

        recordGuess(session, round, player, HangmanGuessType.WORD, guess, correct, delta);

        if (correct) {
            closeRound(session, round, HangmanRoundStatus.SOLVED, player.getUserEmail(), player.getDisplayName());
        } else if (myTurn) {
            // Ceza yok, adam asılmaz — kaybedilen tek şey sıra.
            passTurn(session);
        }
        // Sırası olmayanın yanlış tahmini sırayı bozmaz: sayaç kaldığı yerden işler.

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Tahmin oylaması — sırası olmayan oyuncu, sırası gelenin seçeceği harfi tahmin eder.
     *
     * Oyuncu başına tek tahmin tutulur; yeniden çağrılırsa harf değişir. Sırası gelen bir
     * harf denediğinde tüm tahminler sonuçlanır ve silinir (bkz. resolvePredictions).
     */
    @Transactional
    public HangmanSessionResponse predictLetter(UUID sessionId, String rawLetter) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        // Takım modunda oylar açık yayınlandığı için tahmin bedava olurdu; yarışta sıra yok.
        assertMode(session, HangmanMode.TURN);
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);

        HangmanParticipant player = assertPlayer(session, email);
        if (email.equals(session.getCurrentTurnEmail())) {
            throw new RuntimeException("Sıra sende — tahmin değil, doğrudan harf seç");
        }

        String letter = normalizeLetter(rawLetter, session.getLanguage());
        if (round.getGuessedLetters().contains(letter)) {
            throw new RuntimeException("Bu harf zaten denendi");
        }

        HangmanPrediction prediction = predictionRepository.findBySessionIdAndUserEmail(sessionId, email)
                .orElseGet(() -> HangmanPrediction.builder()
                        .session(session)
                        .userEmail(email)
                        .displayName(player.getDisplayName())
                        .build());
        prediction.setLetter(letter);
        predictionRepository.save(prediction);

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Ara ekranının geri sayımını durdurur — masada kelime üzerine sohbet uzadığında.
     * Sonraki kelimeye geçmek yine moderatörün butonuyla olur. Sadece moderatör.
     */
    @Transactional
    public HangmanSessionResponse pauseIntermission(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertHost(session, email);

        if (!Boolean.TRUE.equals(session.getAwaitingNextRound())) {
            throw new RuntimeException("Şu an tur arasında değilsiniz");
        }
        session.setIntermissionEndsAt(null);
        sessionRepository.save(session);

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    // ─── Takım modu ─────────────────────────────────────────────────────────────

    /**
     * Takım modunda harf oyu — yalnızca sırası gelen takımın üyeleri verebilir.
     *
     * Oyuncu başına tek oy tutulur; yeniden çağrılırsa oy değişir. Takımın tüm üyeleri oy
     * verdiğinde en çok oy alan harf hemen oynanır; vermeyen varsa sıra süresi dolunca oynanır.
     */
    @Transactional
    public HangmanSessionResponse teamVote(UUID sessionId, String rawLetter) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertMode(session, HangmanMode.TEAM);
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);

        HangmanParticipant player = assertPlayer(session, email);
        if (!Objects.equals(player.getTeamNo(), session.getCurrentTeam())) {
            throw new RuntimeException("Sıra rakip takımda — kendi sıranızı bekleyin");
        }

        String letter = normalizeLetter(rawLetter, session.getLanguage());
        if (round.getGuessedLetters().contains(letter)) {
            throw new RuntimeException("Bu harf zaten denendi");
        }

        HangmanTeamVote vote = teamVoteRepository.findBySessionIdAndUserEmail(sessionId, email)
                .orElseGet(() -> HangmanTeamVote.builder()
                        .session(session)
                        .userEmail(email)
                        .displayName(player.getDisplayName())
                        .build());
        vote.setLetter(letter);
        teamVoteRepository.save(vote);

        long teamSize = players(sessionId).stream()
                .filter(p -> Objects.equals(p.getTeamNo(), session.getCurrentTeam()))
                .count();
        if (teamVoteRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).size() >= teamSize) {
            applyTeamVotes(session, round);
        }

        HangmanSessionResponse response = buildResponse(reload(sessionId));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Sırası gelen takımın oylarını sonuçlandırır: en çok oy alan harf (eşitlikte önce oy alan)
     * oynanır. Puan hem takıma hem o harfe oy veren her üyeye yazılır. Hiç oy yoksa sıra rakibe geçer.
     */
    private void applyTeamVotes(HangmanSession session, HangmanRound round) {
        List<HangmanTeamVote> votes = teamVoteRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        if (votes.isEmpty()) {
            advanceTeam(session);
            return;
        }

        String letter = tallyVotes(votes).get(0).letter();
        List<String> voterEmails = votes.stream()
                .filter(v -> letter.equals(v.getLetter()))
                .map(HangmanTeamVote::getUserEmail)
                .toList();

        boolean hit = round.getWord().contains(letter);
        int streakBefore = hit ? lettersThisTurn(session, round, null) : 0;
        round.getGuessedLetters().add(letter);
        if (!hit) {
            round.setWrongCount(round.getWrongCount() + 1);
        }
        int delta = hit ? SCORE_CORRECT_LETTER : SCORE_WRONG_LETTER;
        boolean solved = isFullyRevealed(round);
        roundRepository.save(round);

        HangmanParticipant firstVoter = null;
        for (String voterEmail : voterEmails) {
            HangmanParticipant voter = participantRepository
                    .findBySessionIdAndUserEmail(session.getId(), voterEmail).orElse(null);
            if (voter == null) continue;
            if (firstVoter == null) firstVoter = voter;
            if (hit) {
                voter.setCorrectLetterCount(voter.getCorrectLetterCount() + 1);
            } else {
                voter.setWrongLetterCount(voter.getWrongLetterCount() + 1);
            }
            if (solved) {
                voter.setWordsSolved(voter.getWordsSolved() + 1);
            }
            voter.setTotalScore(voter.getTotalScore() + delta);
            participantRepository.save(voter);
        }

        Integer team = session.getCurrentTeam();
        addTeamScore(session, team, delta);
        String teamName = teamName(team);
        if (firstVoter != null) {
            // Canlı akışta hamle takımın adıyla görünür; kayıt yine bir oyuncuya bağlı olmalı.
            guessRepository.save(HangmanGuess.builder()
                    .session(session)
                    .round(round)
                    .userEmail(firstVoter.getUserEmail())
                    .displayName(teamName + " takım")
                    .guessType(HangmanGuessType.LETTER)
                    .value(letter)
                    .correct(hit)
                    .scoreDelta(delta)
                    .build());
        }
        teamVoteRepository.deleteBySessionId(session.getId());

        if (solved) {
            closeRound(session, round, HangmanRoundStatus.SOLVED,
                    firstVoter == null ? null : firstVoter.getUserEmail(), teamName + " takım");
        } else if (round.getWrongCount() >= MAX_WRONG) {
            closeRound(session, round, HangmanRoundStatus.FAILED, null, null);
        } else if (!hit || streakBefore + 1 >= MAX_STREAK) {
            advanceTeam(session);
        }
        // Aksi hâlde sıra takımda kalır; süre kaldığı yerden işler, takım yeniden oylar.
    }

    /** Oyları harf harf sayar: çok oy alan önce, eşitlikte ilk oyu önce alan önce. */
    private List<HangmanTeamVoteTally> tallyVotes(List<HangmanTeamVote> votesInOrder) {
        Map<String, List<String>> byLetter = new LinkedHashMap<>();
        for (HangmanTeamVote v : votesInOrder) {
            byLetter.computeIfAbsent(v.getLetter(), k -> new ArrayList<>()).add(v.getDisplayName());
        }
        List<HangmanTeamVoteTally> tallies = new ArrayList<>();
        byLetter.forEach((letter, voters) -> tallies.add(new HangmanTeamVoteTally(letter, voters.size(), voters)));
        // Kararlı sıralama: LinkedHashMap ilk oy sırasını koruduğu için eşitlikte o kazanır.
        tallies.sort(Comparator.comparingInt(HangmanTeamVoteTally::count).reversed());
        return tallies;
    }

    private String teamName(Integer teamNo) {
        return teamNo == null ? "" : TEAM_NAMES[teamNo - 1];
    }

    // ─── Yarış modu ─────────────────────────────────────────────────────────────

    /** Yarışta oyuncunun kendi tahtası — yalnızca sahibine döner. */
    public HangmanRaceBoardResponse getMyRaceBoard(UUID sessionId) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertMode(session, HangmanMode.RACE);
        HangmanRound round = roundRepository.findBySessionIdAndRoundOrder(sessionId, session.getCurrentRoundIndex())
                .orElseThrow(() -> new RuntimeException("Aktif tur yok"));
        HangmanRaceBoard board = raceBoardRepository.findByRoundIdAndUserEmail(round.getId(), email).orElse(null);
        return HangmanRaceBoardResponse.from(round, board, MAX_WRONG);
    }

    /** Yarışta kendi tahtana harf tahmini. Yanlış harf can götürür; puan yalnızca çözünce gelir. */
    @Transactional
    public HangmanRaceBoardResponse raceGuessLetter(UUID sessionId, String rawLetter) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertMode(session, HangmanMode.RACE);
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);
        HangmanParticipant player = assertPlayer(session, email);
        HangmanRaceBoard board = activeRaceBoard(session, round, player);

        String letter = normalizeLetter(rawLetter, session.getLanguage());
        if (board.getGuessedLetters().contains(letter)) {
            throw new RuntimeException("Bu harfi zaten denedin");
        }
        board.getGuessedLetters().add(letter);

        if (round.getWord().contains(letter)) {
            player.setCorrectLetterCount(player.getCorrectLetterCount() + 1);
        } else {
            player.setWrongLetterCount(player.getWrongLetterCount() + 1);
            board.setWrongCount(board.getWrongCount() + 1);
        }

        boolean solved = round.getWord().chars()
                .allMatch(c -> board.getGuessedLetters().contains(String.valueOf((char) c)));
        return finishRaceMove(session, round, player, board, solved);
    }

    /** Yarışta kendi tahtana kelime tahmini. Yanlış tahmin bir can götürür. */
    @Transactional
    public HangmanRaceBoardResponse raceGuessWord(UUID sessionId, String rawWord) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        assertMode(session, HangmanMode.RACE);
        HangmanRound round = activeRound(session);
        assertTurnNotExpired(session);
        HangmanParticipant player = assertPlayer(session, email);
        HangmanRaceBoard board = activeRaceBoard(session, round, player);

        String guess = rawWord == null ? "" : rawWord.trim().toLowerCase(localeOf(session.getLanguage()));
        if (guess.isEmpty()) {
            throw new RuntimeException("Tahmin boş olamaz");
        }

        boolean correct = round.getWord().equals(guess);
        if (correct) {
            for (char c : round.getWord().toCharArray()) {
                String ch = String.valueOf(c);
                if (!board.getGuessedLetters().contains(ch)) board.getGuessedLetters().add(ch);
            }
        } else {
            // Ceza olmasa yarışta kelime deneme yanılmayla kırılırdı.
            board.setWrongCount(board.getWrongCount() + 1);
        }
        return finishRaceMove(session, round, player, board, correct);
    }

    /** Tahtayı kaydeder, çözüldüyse puanlar, herkes bittiyse turu kapatır ve yayınlar. */
    private HangmanRaceBoardResponse finishRaceMove(HangmanSession session, HangmanRound round,
                                                    HangmanParticipant player, HangmanRaceBoard board,
                                                    boolean solved) {
        if (solved) {
            int place = (int) raceBoardRepository.findByRoundId(round.getId()).stream()
                    .filter(b -> b.getStatus() == HangmanRoundStatus.SOLVED)
                    .count() + 1;
            int score = raceSolveScore(place, board.getWrongCount());
            board.setStatus(HangmanRoundStatus.SOLVED);
            board.setFinishOrder(place);
            board.setScoreEarned(score);
            board.setFinishedAt(LocalDateTime.now());
            player.setTotalScore(player.getTotalScore() + score);
            player.setWordsSolved(player.getWordsSolved() + 1);
        } else if (board.getWrongCount() >= MAX_WRONG) {
            board.setStatus(HangmanRoundStatus.FAILED);
            board.setFinishedAt(LocalDateTime.now());
        }
        raceBoardRepository.save(board);
        participantRepository.save(player);

        if (allRaceBoardsDone(session, round)) {
            closeRaceRound(session, round);
        }

        broadcast(session.getTeam().getId(), buildResponse(reload(session.getId())));
        return HangmanRaceBoardResponse.from(round, board, MAX_WRONG);
    }

    /** Oyuncunun bu turdaki tahtası; yoksa oluşturulur. Bitmiş tahtada oynanamaz. */
    private HangmanRaceBoard activeRaceBoard(HangmanSession session, HangmanRound round, HangmanParticipant player) {
        HangmanRaceBoard board = raceBoardRepository.findByRoundIdAndUserEmail(round.getId(), player.getUserEmail())
                .orElseGet(() -> HangmanRaceBoard.builder()
                        .session(session)
                        .round(round)
                        .userEmail(player.getUserEmail())
                        .displayName(player.getDisplayName())
                        .build());
        if (board.getStatus() != HangmanRoundStatus.ACTIVE) {
            throw new RuntimeException("Bu kelimeyi bitirdin — diğerlerini bekle");
        }
        return board;
    }

    /** Bitiş sırasına göre puan + kalan her can için bonus. */
    static int raceSolveScore(int place, int wrongCount) {
        int base = place <= RACE_PLACE_SCORES.length ? RACE_PLACE_SCORES[place - 1] : RACE_MIN_SOLVE_SCORE;
        return base + Math.max(0, MAX_WRONG - wrongCount) * RACE_LIFE_BONUS;
    }

    /** Oynayan herkes (izleyiciler hariç) bu kelimeyi bitirdi mi — çözerek ya da asılarak. */
    private boolean allRaceBoardsDone(HangmanSession session, HangmanRound round) {
        Map<String, HangmanRaceBoard> boards = new HashMap<>();
        raceBoardRepository.findByRoundId(round.getId()).forEach(b -> boards.put(b.getUserEmail(), b));
        return players(session.getId()).stream().allMatch(p -> {
            HangmanRaceBoard b = boards.get(p.getUserEmail());
            return b != null && b.getStatus() != HangmanRoundStatus.ACTIVE;
        });
    }

    /** Yarış turunu kapatır: ilk bitiren turu "çözen" sayılır; kimse çözemediyse tur başarısız. */
    private void closeRaceRound(HangmanSession session, HangmanRound round) {
        HangmanRaceBoard winner = raceBoardRepository.findByRoundId(round.getId()).stream()
                .filter(b -> b.getStatus() == HangmanRoundStatus.SOLVED && b.getFinishOrder() != null)
                .min(Comparator.comparingInt(HangmanRaceBoard::getFinishOrder))
                .orElse(null);
        closeRound(session, round,
                winner == null ? HangmanRoundStatus.FAILED : HangmanRoundStatus.SOLVED,
                winner == null ? null : winner.getUserEmail(),
                winner == null ? null : winner.getDisplayName());
    }

    // ─── Tepkiler ───────────────────────────────────────────────────────────────

    /**
     * Anlık emoji tepkisi gönderir. Saklanmaz; yalnızca /topic/hangman/{teamId}/reactions
     * kanalına yayınlanır. Çok sık gönderilen tepkiler sessizce yutulur.
     */
    public void react(UUID sessionId, String emoji) {
        String email = currentEmail();
        HangmanSession session = findSession(sessionId);
        if (session.getStatus() != HangmanSessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Oyun aktif değil");
        }
        if (emoji == null || !REACTION_EMOJIS.contains(emoji)) {
            throw new IllegalArgumentException("Geçersiz tepki");
        }
        HangmanParticipant participant = participantRepository.findBySessionIdAndUserEmail(sessionId, email)
                .orElseThrow(() -> new RuntimeException("Bu oyuna katılmadınız"));

        long now = System.currentTimeMillis();
        String key = sessionId + "|" + email;
        Long last = lastReactionAt.get(key);
        if (last != null && now - last < REACTION_MIN_INTERVAL_MS) return;
        lastReactionAt.put(key, now);

        try {
            messagingTemplate.convertAndSend("/topic/hangman/" + session.getTeam().getId() + "/reactions",
                    new HangmanReactionEvent(sessionId.toString(), email, participant.getDisplayName(), emoji));
        } catch (Exception e) {
            log.warn("[WS] Hangman tepki yayını başarısız: {}", e.getMessage());
        }
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
        if (mode(session) == HangmanMode.RACE) {
            throw new RuntimeException("Yarış modunda devredilecek bir sıra yok");
        }
        passTurn(session);

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
     * Turu kapatır ve oyunu ara ekranına alır: kelime herkese açılır. Otomatik geçiş açıksa
     * {@link #INTERMISSION_SECONDS} sn sonra zamanlayıcı sonraki tura geçer; kapalıysa ya da
     * moderatör duraklattıysa {@link #nextRound(UUID)} çağrısı beklenir.
     *
     * Ara ekranının amacı cevabın bir an yanıp sönmesi yerine masadaki herkesin "kelime
     * neymiş" diye görebilmesi; geri sayım ise moderatör dalgınken herkesin beklememesi için.
     * Ara boyunca {@code currentRoundIndex} biten turda kalır (yanıt cevabı açık taşısın),
     * {@code currentTurnEmail} de turu bitiren oyuncuda durur — sonraki tur onun
     * sonrasındaki oyuncuyla başlasın diye.
     */
    private void closeRound(HangmanSession session, HangmanRound round,
                            HangmanRoundStatus status, String solverEmail, String solverName) {
        round.setStatus(status);
        round.setFinishedAt(LocalDateTime.now());
        if (solverEmail != null) {
            round.setSolvedByEmail(solverEmail);
            round.setSolvedByName(solverName);
        }
        roundRepository.save(round);

        session.setAwaitingNextRound(true);
        session.setIntermissionEndsAt(Boolean.TRUE.equals(session.getAutoAdvance())
                ? LocalDateTime.now().plusSeconds(INTERMISSION_SECONDS)
                : null);
        sessionRepository.save(session);
        predictionRepository.deleteBySessionId(session.getId());
        teamVoteRepository.deleteBySessionId(session.getId());
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
        if (sessionRepository.claimIntermissionExit(sessionId) == 0) {
            // Otomatik geçiş aynı anda davrandı; güncel durum WS ile zaten yayınlandı.
            throw new RuntimeException("Sonraki kelimeye zaten geçildi");
        }
        return openNextRound(session);
    }

    /**
     * Ara ekranından çıkar: sonraki turu açar ya da tur kalmadıysa oyunu bitirir.
     * Çağıran, çıkışı {@link HangmanSessionRepository#claimIntermissionExit} ile sahiplenmiş olmalı.
     */
    private HangmanSessionResponse openNextRound(HangmanSession session) {
        // Toplu güncelleme bellekteki nesneye yansımaz; sonraki save eski değeri geri yazmasın.
        session.setAwaitingNextRound(false);
        session.setIntermissionEndsAt(null);

        int nextIndex = session.getCurrentRoundIndex() + 1;
        Optional<HangmanRound> next = roundRepository.findBySessionIdAndRoundOrder(session.getId(), nextIndex);

        if (next.isEmpty()) {
            // Son kelimeydi: ara ekranından sonra sonuçlar gelir.
            return doFinish(session);
        }

        HangmanRound nextRound = next.get();
        nextRound.setStatus(HangmanRoundStatus.ACTIVE);
        nextRound.setStartedAt(LocalDateTime.now());
        roundRepository.save(nextRound);

        session.setCurrentRoundIndex(nextIndex);
        sessionRepository.save(session);

        startRoundTurn(session);

        HangmanSessionResponse response = buildResponse(reload(session.getId()));
        broadcast(session.getTeam().getId(), response);
        return response;
    }

    /**
     * Yeni turun (kelimenin) ilk sırasını kurar:
     *   • sıralıda turu bitiren oyuncunun sonrasındaki oyuncu (oyun başında ilk oyuncu),
     *   • takımda turu bitiren takımın rakibi (oyun başında 1. takım),
     *   • yarışta sıra yok — sayaç kelimenin yarış süresini saymaya başlar.
     */
    private void startRoundTurn(HangmanSession session) {
        switch (mode(session)) {
            case TURN -> advanceTurn(session, session.getCurrentTurnEmail());
            case TEAM -> advanceTeam(session);
            case RACE -> {
                session.setCurrentTurnEmail(null);
                session.setTurnStartedAt(LocalDateTime.now());
                sessionRepository.save(session);
            }
        }
    }

    /** Sırayı moda göre devreder: sıralıda sonraki oyuncuya, takımda rakip takıma. */
    private void passTurn(HangmanSession session) {
        if (mode(session) == HangmanMode.TEAM) {
            advanceTeam(session);
        } else {
            advanceTurn(session, session.getCurrentTurnEmail());
        }
    }

    /**
     * Takım sırasını rakip takıma geçirir; rakipte kimse kalmadıysa aynı takımda kalır.
     * Oyun başında (currentTeam null) 1. takımla başlar.
     */
    private void advanceTeam(HangmanSession session) {
        List<HangmanParticipant> players = players(session.getId());
        Integer current = session.getCurrentTeam();
        int preferred = current == null ? 1 : 3 - current;

        Integer next = null;
        for (int candidate : new int[]{preferred, 3 - preferred}) {
            if (players.stream().anyMatch(p -> Objects.equals(p.getTeamNo(), candidate))) {
                next = candidate;
                break;
            }
        }

        session.setCurrentTeam(next);
        session.setTurnStartedAt(next == null ? null : LocalDateTime.now());
        sessionRepository.save(session);
        // Oylar bir önceki takımın hamlesi içindi.
        teamVoteRepository.deleteBySessionId(session.getId());
    }

    /** Oyun sürerken katılan oyuncunun gireceği takım: üyesi az olan (eşitse 1). */
    private int smallerTeam(UUID sessionId) {
        List<HangmanParticipant> players = players(sessionId);
        long one = players.stream().filter(p -> Objects.equals(p.getTeamNo(), 1)).count();
        long two = players.stream().filter(p -> Objects.equals(p.getTeamNo(), 2)).count();
        return two < one ? 2 : 1;
    }

    /** Takım modunda takım puanına ekler; diğer modlarda (teamNo null) bir şey yapmaz. */
    private void addTeamScore(HangmanSession session, Integer teamNo, int delta) {
        if (mode(session) != HangmanMode.TEAM || teamNo == null || delta == 0) return;
        if (teamNo == 1) {
            session.setTeamOneScore(teamScore(session, 1) + delta);
        } else {
            session.setTeamTwoScore(teamScore(session, 2) + delta);
        }
        sessionRepository.save(session);
    }

    private int teamScore(HangmanSession session, int teamNo) {
        Integer score = teamNo == 1 ? session.getTeamOneScore() : session.getTeamTwoScore();
        return score == null ? 0 : score;
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
        // Yeni sıra = yeni süre: sıra sayacı ve kelime kilidi baştan başlar.
        session.setTurnStartedAt(LocalDateTime.now());
        sessionRepository.save(session);
        // Tahminler bir önceki oyuncunun hamlesi içindi; yeni sırada herkes yeniden seçer.
        predictionRepository.deleteBySessionId(session.getId());
    }

    /**
     * Süresi dolan sıraları devreder — kimse tahmin etmediğinde oyunun takılmaması için.
     * Geri sayımı biten ara ekranlarını da kapatır (otomatik geçiş).
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
                if (isIntermissionOver(session)) {
                    // Moderatör aynı anda "Sonraki Kelime"ye bastıysa o kazanır, burası çekilir.
                    if (sessionRepository.claimIntermissionExit(session.getId()) == 1) {
                        openNextRound(session);
                    }
                    continue;
                }
                if (Boolean.TRUE.equals(session.getAwaitingNextRound()) || !hasActiveTurn(session)) continue;

                // Sürüm öncesinde başlamış oturumlarda sayaç boştur — şimdiden başlat.
                if (session.getTurnStartedAt() == null) {
                    session.setTurnStartedAt(LocalDateTime.now());
                    sessionRepository.save(session);
                    continue;
                }
                if (!isTurnExpired(session)) continue;

                switch (mode(session)) {
                    case TURN -> advanceTurn(session, session.getCurrentTurnEmail());
                    // Oy verildiyse en çok oy alan harf oynanır; verilmediyse sıra rakibe geçer.
                    case TEAM -> applyTeamVotes(session, activeRound(session));
                    // Yarış süresi doldu: bitiremeyenler puan almadan tur kapanır.
                    case RACE -> closeRaceRound(session, activeRound(session));
                }
                broadcast(session.getTeam().getId(), buildResponse(reload(session.getId())));
            } catch (Exception e) {
                log.warn("[Hangman] Süre dolmuş sıra/tur arası ilerletilemedi ({}): {}", session.getId(), e.getMessage());
            }
        }
    }

    private HangmanSessionResponse doFinish(HangmanSession session) {
        session.setStatus(HangmanSessionStatus.FINISHED);
        session.setFinishedAt(LocalDateTime.now());
        session.setCurrentTurnEmail(null);
        session.setTurnStartedAt(null);
        session.setAwaitingNextRound(false);
        session.setIntermissionEndsAt(null);
        session.setCurrentTeam(null);
        sessionRepository.save(session);
        predictionRepository.deleteBySessionId(session.getId());
        teamVoteRepository.deleteBySessionId(session.getId());

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
        recordGuess(session, round, player, type, value, correct, scoreDelta, List.of());
    }

    private void recordGuess(HangmanSession session, HangmanRound round, HangmanParticipant player,
                             HangmanGuessType type, String value, boolean correct, int scoreDelta,
                             List<String> predictionWinners) {
        guessRepository.save(HangmanGuess.builder()
                .session(session)
                .round(round)
                .userEmail(player.getUserEmail())
                .displayName(player.getDisplayName())
                .guessType(type)
                .value(value)
                .correct(correct)
                .scoreDelta(scoreDelta)
                .predictionWinners(predictionWinners.isEmpty() ? null : String.join("\n", predictionWinners))
                .build());
    }

    /**
     * Sırası gelenin harf hamlesinden sonra bekleyen tahminleri sonuçlandırır ve siler.
     * Aynı harfi seçenler, harf kelimede varsa {@link #SCORE_PREDICTION} puan alır.
     *
     * @return bonus alanların adları (canlı akışta gösterilir)
     */
    private List<String> resolvePredictions(HangmanSession session, String letter, boolean hit) {
        List<String> winners = new ArrayList<>();
        if (hit) {
            for (HangmanPrediction prediction : predictionRepository.findBySessionId(session.getId())) {
                if (!letter.equals(prediction.getLetter())) continue;
                participantRepository.findBySessionIdAndUserEmail(session.getId(), prediction.getUserEmail())
                        .ifPresent(p -> {
                            p.setPredictionHits((p.getPredictionHits() == null ? 0 : p.getPredictionHits()) + 1);
                            p.setTotalScore(p.getTotalScore() + SCORE_PREDICTION);
                            participantRepository.save(p);
                            winners.add(p.getDisplayName());
                        });
            }
        }
        predictionRepository.deleteBySessionId(session.getId());
        return winners;
    }

    /**
     * Sırası gelenin bu sıra penceresinde (turnStartedAt'ten beri) bu turda bulduğu doğru harf
     * sayısı. {@code email} null ise kim oynadığına bakılmaz — takım modunda sıra takımındır.
     */
    private int lettersThisTurn(HangmanSession session, HangmanRound round, String email) {
        LocalDateTime turnStart = session.getTurnStartedAt();
        if (turnStart == null) return 0;
        return (int) guessRepository.findByRoundIdOrderByCreatedAtAsc(round.getId()).stream()
                .filter(g -> g.getGuessType() == HangmanGuessType.LETTER
                        && (email == null || email.equals(g.getUserEmail()))
                        && Boolean.TRUE.equals(g.getCorrect())
                        && g.getCreatedAt() != null
                        && !g.getCreatedAt().isBefore(turnStart))
                .count();
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

    /** Oyunda oynayan (izleyici olmayan) katılımcıyı döner. */
    private HangmanParticipant assertPlayer(HangmanSession session, String email) {
        HangmanParticipant player = participantRepository
                .findBySessionIdAndUserEmail(session.getId(), email)
                .orElseThrow(() -> new RuntimeException("Bu oyuna katılmadınız"));
        if (Boolean.TRUE.equals(player.getSpectator())) {
            throw new RuntimeException("Kelimeleri siz belirlediğiniz için bu oyunda oynayamazsınız");
        }
        return player;
    }

    private void assertMode(HangmanSession session, HangmanMode expected) {
        if (mode(session) != expected) {
            throw new RuntimeException("Bu hamle bu oyun modunda geçerli değil");
        }
    }

    private HangmanMode mode(HangmanSession session) {
        return HangmanMode.of(session.getMode());
    }

    /** Aktif bir sıra var ve süresi doldu mu? turnStartedAt boşsa (eski oturum) süre işlemez. */
    private boolean isTurnExpired(HangmanSession session) {
        return session.getStatus() == HangmanSessionStatus.IN_PROGRESS
                && !Boolean.TRUE.equals(session.getAwaitingNextRound())
                && hasActiveTurn(session)
                && session.getTurnStartedAt() != null
                && elapsedTurnSeconds(session) >= turnLength(session);
    }

    /** Sayacı işleyen bir sıra var mı? Yarışta tur boyunca hep vardır (kelimenin süresi). */
    private boolean hasActiveTurn(HangmanSession session) {
        return switch (mode(session)) {
            case TURN -> session.getCurrentTurnEmail() != null;
            case TEAM -> session.getCurrentTeam() != null;
            case RACE -> true;
        };
    }

    /** Sayacın süresi: yarışta bir kelimenin süresi, diğerlerinde bir sıranın süresi. */
    private int turnLength(HangmanSession session) {
        return mode(session) == HangmanMode.RACE ? pace(session).raceSeconds() : pace(session).turnSeconds();
    }

    /** Ara ekranının geri sayımı bitti mi? Geri sayım yoksa (kapalı/duraklatıldı) hiç bitmez. */
    private boolean isIntermissionOver(HangmanSession session) {
        return Boolean.TRUE.equals(session.getAwaitingNextRound())
                && session.getIntermissionEndsAt() != null
                && !LocalDateTime.now().isBefore(session.getIntermissionEndsAt());
    }

    /** Ara ekranının kapanmasına kalan saniye; geri sayım işlemiyorsa null. */
    private Integer intermissionSecondsLeft(HangmanSession session) {
        if (!Boolean.TRUE.equals(session.getAwaitingNextRound()) || session.getIntermissionEndsAt() == null) {
            return null;
        }
        return (int) Math.max(0, Duration.between(LocalDateTime.now(), session.getIntermissionEndsAt()).getSeconds());
    }

    private HangmanPace pace(HangmanSession session) {
        return HangmanPace.of(session.getPace());
    }

    /** Mevcut sıranın başlamasından bu yana geçen saniye. */
    private long elapsedTurnSeconds(HangmanSession session) {
        LocalDateTime start = session.getTurnStartedAt();
        if (start == null) return 0;
        return Math.max(0, Duration.between(start, LocalDateTime.now()).getSeconds());
    }

    /** Sıranın bitmesine kalan saniye; aktif sıra yoksa (ya da tur arasıysa) 0. */
    private int turnSecondsLeft(HangmanSession session) {
        if (session.getStatus() != HangmanSessionStatus.IN_PROGRESS || !hasActiveTurn(session)
                || Boolean.TRUE.equals(session.getAwaitingNextRound())) {
            return 0;
        }
        return (int) Math.max(0, turnLength(session) - elapsedTurnSeconds(session));
    }

    /** Sıranın gerçekten bu oyuncuda olduğunu doğrular. */
    private HangmanParticipant assertTurn(HangmanSession session, String email) {
        HangmanParticipant player = assertPlayer(session, email);
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
                    p.getWrongLetterCount(),
                    p.getPredictionHits() == null ? 0 : p.getPredictionHits(),
                    p.getTeamNo()
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
                turnLength(session), turnSecondsLeft(session),
                MAX_STREAK, intermissionSecondsLeft(session), SCORE_PREDICTION,
                (int) predictionRepository.countBySessionId(sessionId),
                SCORE_WRONG_WORD,
                teams(session, all),
                mode(session) == HangmanMode.TEAM
                        ? tallyVotes(teamVoteRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                        : List.of(),
                raceProgress(session, all, rounds),
                rounds.size(), participants, leaderboard, recentGuesses);
    }

    private List<HangmanTeamResponse> teams(HangmanSession session, List<HangmanParticipant> all) {
        if (mode(session) != HangmanMode.TEAM) return List.of();
        List<HangmanTeamResponse> teams = new ArrayList<>();
        for (int teamNo = 1; teamNo <= 2; teamNo++) {
            int no = teamNo;
            int members = (int) all.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getSpectator()) && Objects.equals(p.getTeamNo(), no))
                    .count();
            teams.add(new HangmanTeamResponse(no, teamName(no), teamScore(session, no), members));
        }
        return teams;
    }

    /**
     * Yarışta oynanan (ya da ara ekranındaysa yeni biten) kelimedeki ilerleme. Önce bitirenler,
     * sonra en çok harf açanlar. Henüz tahmin yapmamış oyuncu da boş ilerlemeyle listelenir.
     */
    private List<HangmanRaceProgress> raceProgress(HangmanSession session, List<HangmanParticipant> all,
                                                   List<HangmanRound> rounds) {
        if (mode(session) != HangmanMode.RACE) return List.of();
        HangmanRound round = rounds.stream()
                .filter(r -> r.getRoundOrder() == session.getCurrentRoundIndex())
                .findFirst().orElse(null);
        if (round == null) return List.of();

        Map<String, HangmanRaceBoard> boards = new HashMap<>();
        raceBoardRepository.findByRoundId(round.getId()).forEach(b -> boards.put(b.getUserEmail(), b));
        String word = round.getWord();

        List<HangmanRaceProgress> progress = new ArrayList<>();
        for (HangmanParticipant p : all) {
            if (Boolean.TRUE.equals(p.getSpectator())) continue;
            HangmanRaceBoard b = boards.get(p.getUserEmail());
            int revealed = 0;
            if (b != null) {
                for (char c : word.toCharArray()) {
                    if (b.getGuessedLetters().contains(String.valueOf(c))) revealed++;
                }
            }
            progress.add(new HangmanRaceProgress(
                    p.getUserEmail(),
                    p.getDisplayName(),
                    revealed,
                    word.length(),
                    b == null ? 0 : b.getWrongCount(),
                    MAX_WRONG,
                    b == null ? HangmanRoundStatus.ACTIVE.name() : b.getStatus().name(),
                    b == null ? null : b.getFinishOrder(),
                    b == null ? 0 : b.getScoreEarned()));
        }
        progress.sort(Comparator
                .comparing((HangmanRaceProgress r) -> r.finishOrder() == null ? Integer.MAX_VALUE : r.finishOrder())
                .thenComparing(Comparator.comparingInt(HangmanRaceProgress::revealedCount).reversed()));
        return progress;
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

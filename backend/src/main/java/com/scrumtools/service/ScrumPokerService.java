package com.scrumtools.service;

import com.scrumtools.dto.PokerSessionResponse;
import com.scrumtools.dto.PokerVoteResponse;
import com.scrumtools.entity.*;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ScrumPoker servisi — Data-Carrying WebSocket stratejisi.
 *
 * Her oy/görünürlük değişikliğinde güncel veri doğrudan WebSocket üzerinden broadcast edilir.
 *   /topic/poker/{teamId}/votes       → tam oy listesi (List<PokerVoteResponse>)
 *   /topic/poker/{teamId}/visibility  → { "votesVisible": true/false }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScrumPokerService {

    private final PokerSessionRepository sessionRepository;
    private final PokerVoteRepository voteRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Session ──────────────────────────────────────────────────────────────

    /**
     * Takımın oturumunu döndürür; yoksa oluşturur.
     */
    public PokerSessionResponse getSession(UUID teamId) {
        PokerSession session = getOrCreateSession(teamId);
        List<PokerVoteResponse> votes = voteRepository.findByTeamId(teamId)
                .stream().map(PokerVoteResponse::from).collect(Collectors.toList());
        return PokerSessionResponse.from(session, votes);
    }

    /**
     * Mevcut oyları döndürür.
     */
    public List<PokerVoteResponse> getVotes(UUID teamId) {
        return voteRepository.findByTeamId(teamId)
                .stream().map(PokerVoteResponse::from).collect(Collectors.toList());
    }

    // ─── Join / Leave ─────────────────────────────────────────────────────────

    /**
     * Kullanıcıyı oturuma katar (oy kaydı oluşturur/günceller, vote="-").
     * displayName TeamMember tablosundan otomatik alınır.
     */
    @Transactional
    public PokerSessionResponse joinPoker(UUID teamId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // displayName'i TeamMember'dan al
        String displayName = teamMemberRepository.findByTeamIdAndEmail(teamId, userEmail)
                .map(TeamMember::getDisplayName)
                .orElse(userEmail);

        // Var olan oyu güncelle ya da yeni oluştur
        PokerVote pokerVote = voteRepository.findByTeamIdAndUserEmail(teamId, userEmail)
                .orElseGet(() -> {
                    Team team = teamRepository.findById(teamId)
                            .orElseThrow(() -> new RuntimeException("Team not found"));
                    return PokerVote.builder().team(team).userEmail(userEmail).build();
                });

        pokerVote.setDisplayName(displayName);
        pokerVote.setVote("-");
        pokerVote.setTimestamp(System.currentTimeMillis());
        voteRepository.save(pokerVote);

        // Oturum bilgisini al/oluştur
        PokerSession session = getOrCreateSession(teamId);

        // Oy listesini broadcast et
        broadcastVotes(teamId);

        List<PokerVoteResponse> votes = voteRepository.findByTeamId(teamId)
                .stream().map(PokerVoteResponse::from).collect(Collectors.toList());
        return PokerSessionResponse.from(session, votes);
    }

    /**
     * Kullanıcıyı oturumdan çıkarır (oy kaydını siler).
     */
    @Transactional
    public void leavePoker(UUID teamId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        voteRepository.deleteByTeamIdAndUserEmail(teamId, userEmail);
        broadcastVotes(teamId);
    }

    // ─── Vote ─────────────────────────────────────────────────────────────────

    /**
     * Kullanıcının oyunu günceller.
     */
    @Transactional
    public PokerVoteResponse vote(UUID teamId, String voteValue) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        PokerVote pokerVote = voteRepository.findByTeamIdAndUserEmail(teamId, userEmail)
                .orElseThrow(() -> new RuntimeException("Önce oturuma katılın (join)"));

        pokerVote.setVote(voteValue);
        pokerVote.setTimestamp(System.currentTimeMillis());
        PokerVote saved = voteRepository.save(pokerVote);

        // Güncel oy listesini broadcast et
        broadcastVotes(teamId);
        return PokerVoteResponse.from(saved);
    }

    // ─── Visibility ───────────────────────────────────────────────────────────

    /**
     * Oyların görünürlüğünü günceller.
     */
    @Transactional
    public void setVotesVisible(UUID teamId, boolean visible) {
        PokerSession session = getOrCreateSession(teamId);
        session.setVotesVisible(visible);
        sessionRepository.save(session);
        broadcastVisibility(teamId, visible);
    }

    // ─── Card Type ────────────────────────────────────────────────────────────

    /**
     * Kart tipini günceller (fibonacci, tshirt, vb.).
     */
    @Transactional
    public void setCardType(UUID teamId, String cardType) {
        PokerSession session = getOrCreateSession(teamId);
        session.setCardType(cardType);
        sessionRepository.save(session);
        // Kart tipi değişikliği için broadcast
        String topic = "/topic/poker/" + teamId + "/card-type";
        try {
            messagingTemplate.convertAndSend(topic, Map.of("cardType", cardType));
        } catch (Exception e) {
            log.warn("[WS] cardType broadcast başarısız: {}", e.getMessage());
        }
    }

    // ─── New Round ────────────────────────────────────────────────────────────

    /**
     * Yeni tur başlatır: tüm oyları "-" yapar, votesVisible=false yapar.
     * Hem oy listesini hem de görünürlüğü broadcast eder.
     */
    @Transactional
    public void newRound(UUID teamId) {
        // Tüm oyları sıfırla
        List<PokerVote> votes = voteRepository.findByTeamId(teamId);
        long now = System.currentTimeMillis();
        votes.forEach(v -> {
            v.setVote("-");
            v.setTimestamp(now);
        });
        voteRepository.saveAll(votes);

        // Görünürlüğü kapat
        PokerSession session = getOrCreateSession(teamId);
        session.setVotesVisible(false);
        sessionRepository.save(session);

        // Her ikisini de broadcast et
        broadcastVotes(teamId);
        broadcastVisibility(teamId, false);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PokerSession getOrCreateSession(UUID teamId) {
        return sessionRepository.findByTeamId(teamId).orElseGet(() -> {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            return sessionRepository.save(PokerSession.builder().team(team).build());
        });
    }

    /**
     * /topic/poker/{teamId}/votes → tam oy listesi JSON
     */
    private void broadcastVotes(UUID teamId) {
        String topic = "/topic/poker/" + teamId + "/votes";
        List<PokerVoteResponse> votes = voteRepository.findByTeamId(teamId)
                .stream().map(PokerVoteResponse::from).collect(Collectors.toList());
        try {
            messagingTemplate.convertAndSend(topic, votes);
        } catch (Exception e) {
            log.warn("[WS] Poker votes broadcast başarısız: topic={}, error={}", topic, e.getMessage());
        }
    }

    /**
     * /topic/poker/{teamId}/visibility → { "votesVisible": true/false }
     */
    private void broadcastVisibility(UUID teamId, boolean visible) {
        String topic = "/topic/poker/" + teamId + "/visibility";
        try {
            messagingTemplate.convertAndSend(topic, Map.of("votesVisible", visible));
        } catch (Exception e) {
            log.warn("[WS] Poker visibility broadcast başarısız: topic={}, error={}", topic, e.getMessage());
        }
    }
}


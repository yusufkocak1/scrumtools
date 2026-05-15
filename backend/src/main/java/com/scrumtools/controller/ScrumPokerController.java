package com.scrumtools.controller;

import com.scrumtools.dto.PokerSessionResponse;
import com.scrumtools.dto.PokerVoteResponse;
import com.scrumtools.service.ScrumPokerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ScrumPoker REST Controller
 *
 * WebSocket stratejisi: Data-Carrying
 *   Yazma işlemleri (join/leave/vote/visibility) REST üzerinden yapılır.
 *   Okuma/dinleme WebSocket subscribe ile yapılır — mesaj doğrudan güncel veriyi taşır.
 *
 *   /topic/poker/{teamId}/votes       → tam oy listesi (her değişiklikte)
 *   /topic/poker/{teamId}/visibility  → { "votesVisible": true/false }
 */
@RestController
@RequestMapping("/api/teams/{teamId}/poker")
@RequiredArgsConstructor
public class ScrumPokerController {

    private final ScrumPokerService scrumPokerService;

    /** Oturum bilgisini ve mevcut oyları döndürür */
    @GetMapping
    public ResponseEntity<PokerSessionResponse> getSession(@PathVariable UUID teamId) {
        return ResponseEntity.ok(scrumPokerService.getSession(teamId));
    }

    /** Mevcut oyları döndürür */
    @GetMapping("/votes")
    public ResponseEntity<List<PokerVoteResponse>> getVotes(@PathVariable UUID teamId) {
        return ResponseEntity.ok(scrumPokerService.getVotes(teamId));
    }

    /** Kullanıcıyı oturuma katar — oy kaydı oluşturulur, vote="-" */
    @PostMapping("/join")
    public ResponseEntity<PokerSessionResponse> join(@PathVariable UUID teamId) {
        return ResponseEntity.ok(scrumPokerService.joinPoker(teamId));
    }

    /** Kullanıcıyı oturumdan çıkarır — oy kaydı silinir */
    @PostMapping("/leave")
    public ResponseEntity<Void> leave(@PathVariable UUID teamId) {
        scrumPokerService.leavePoker(teamId);
        return ResponseEntity.noContent().build();
    }

    /** Kullanıcının oyunu günceller */
    @PostMapping("/vote")
    public ResponseEntity<PokerVoteResponse> vote(
            @PathVariable UUID teamId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(scrumPokerService.vote(teamId, body.get("vote")));
    }

    /** Oyların görünürlüğünü günceller */
    @PatchMapping("/visibility")
    public ResponseEntity<Void> setVotesVisible(
            @PathVariable UUID teamId,
            @RequestBody Map<String, Boolean> body) {
        scrumPokerService.setVotesVisible(teamId, body.get("votesVisible"));
        return ResponseEntity.noContent().build();
    }

    /** Kart tipini günceller */
    @PatchMapping("/card-type")
    public ResponseEntity<Void> setCardType(
            @PathVariable UUID teamId,
            @RequestBody Map<String, String> body) {
        scrumPokerService.setCardType(teamId, body.get("cardType"));
        return ResponseEntity.noContent().build();
    }

    /**
     * Yeni tur başlatır: tüm oyları "-" yapar + votesVisible=false yapar.
     * WebSocket üzerinden hem oy listesi hem görünürlük broadcast edilir.
     */
    @PostMapping("/new-round")
    public ResponseEntity<Void> newRound(@PathVariable UUID teamId) {
        scrumPokerService.newRound(teamId);
        return ResponseEntity.noContent().build();
    }
}


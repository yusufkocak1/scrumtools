package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adam Asmaca takım oturumu.
 *
 * Oturumu başlatan kişi moderatördür (hostEmail). Turlar sırayla oynanır:
 * ortak bir kelime herkese gösterilir, sırası gelen oyuncu ya tek harf ya da
 * kelimenin tamamını tahmin eder.
 *
 * ÖNEMLİ: Kelimenin kendisi bu tabloda (HangmanRound.word) tutulur ve
 * istemciye ASLA açık gönderilmez — sadece maskelenmiş hali gider.
 */
@Entity
@Table(name = "hangman_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String hostEmail;

    @Column
    private String hostName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HangmanSessionStatus status = HangmanSessionStatus.LOBBY;

    /** "tr" | "en" */
    @Column(nullable = false, length = 2)
    @Builder.Default
    private String language = "tr";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HangmanWordSource wordSource = HangmanWordSource.RANDOM;

    /**
     * Moderatör de oyuncu olarak sıraya giriyor mu?
     * wordSource = CUSTOM ise adalet gereği her zaman false'tur.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean moderatorPlays = true;

    /** Şu an oynanan tur index'i (0-based). -1 = henüz başlamadı. */
    @Column(nullable = false)
    @Builder.Default
    private Integer currentRoundIndex = -1;

    /** Sırası gelen oyuncunun e-postası. Oyun başlamadıysa null. */
    @Column
    private String currentTurnEmail;

    // Turlar bilinçli olarak @OneToMany ile bağlanmadı: HangmanRound kayıtları
    // repository üzerinden yazılıyor, bu yüzden buradaki koleksiyon bayat kalırdı.
    // Turlara her zaman HangmanRoundRepository üzerinden erişin.

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;
}

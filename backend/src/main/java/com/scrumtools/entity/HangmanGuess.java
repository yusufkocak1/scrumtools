package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tek bir tahmin kaydı — canlı akış ("Ayşe 'K' dedi, +10") ve oturum raporu için.
 */
@Entity
@Table(name = "hangman_guesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanGuess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private HangmanSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private HangmanRound round;

    @Column(nullable = false)
    private String userEmail;

    @Column
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HangmanGuessType guessType;

    /** Tahmin edilen harf ya da kelime (küçük harf). */
    @Column(nullable = false)
    private String value;

    @Column(nullable = false)
    private Boolean correct;

    /** Bu tahminin puana etkisi (negatif olabilir). */
    @Column(nullable = false)
    @Builder.Default
    private Integer scoreDelta = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

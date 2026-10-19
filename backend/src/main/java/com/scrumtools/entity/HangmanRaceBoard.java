package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Yarış modunda bir oyuncunun bir turdaki kendi tahtası.
 *
 * GÜVENLİK: Tahtanın harfleri yalnızca sahibine döner (bkz. HangmanRaceBoardResponse);
 * herkese yayınlanan durumda sadece ilerleme sayıları bulunur — aksi hâlde yavaş olan,
 * hızlı olanın açtığı harfleri okuyup kopyalayabilirdi.
 */
@Entity
@Table(name = "hangman_race_boards",
        uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "user_email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanRaceBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private HangmanSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private HangmanRound round;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column
    private String displayName;

    /** Bu oyuncunun denediği harfler (doğru + yanlış), küçük harf. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hangman_race_board_letters", joinColumns = @JoinColumn(name = "board_id"))
    @Column(name = "letter", length = 4)
    @Builder.Default
    private List<String> guessedLetters = new ArrayList<>();

    /** Yanlış harf + yanlış kelime tahmini sayısı — adamın parçaları. */
    @Column(nullable = false)
    @Builder.Default
    private Integer wrongCount = 0;

    /** ACTIVE (oynuyor) | SOLVED | FAILED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private HangmanRoundStatus status = HangmanRoundStatus.ACTIVE;

    /** Çözenler arasındaki bitiş sırası (1 = ilk). Çözemediyse null. */
    @Column
    private Integer finishOrder;

    /** Bu turdan kazandığı puan. */
    @Column(nullable = false)
    @Builder.Default
    private Integer scoreEarned = 0;

    @Column
    private LocalDateTime finishedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

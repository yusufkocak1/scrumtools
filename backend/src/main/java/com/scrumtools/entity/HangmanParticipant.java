package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adam Asmaca oturumuna katılan kullanıcı.
 *
 * Takımın tüm üyeleri katılmak zorunda değildir — sadece katılanlar sıraya girer.
 * Oyun başladıktan sonra katılanlar sıranın sonuna eklenir (turnOrder artan şekilde verilir).
 *
 * spectator = true olan katılımcı sıraya girmez ve sıralamada gösterilmez;
 * kelimeleri kendisi belirleyen moderatör bu moddadır.
 */
@Entity
@Table(name = "hangman_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private HangmanSession session;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column
    private String displayName;

    /** Tur sırasındaki yeri. spectator ise anlamsızdır. */
    @Column(nullable = false)
    @Builder.Default
    private Integer turnOrder = 0;

    /** Sıraya girmeyen izleyici (kelimeleri belirleyen moderatör). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean spectator = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer correctLetterCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer wrongLetterCount = 0;

    /** Kelimeyi bitirme sayısı — sadece istatistik; puanı doğrudan etkilemez. */
    @Column(nullable = false)
    @Builder.Default
    private Integer wordsSolved = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;
}

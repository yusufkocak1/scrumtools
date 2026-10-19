package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Takım modunda sırası gelen takımın bir üyesinin harf oyu.
 *
 * Takımın tüm üyeleri oy verdiğinde ya da sıra süresi dolduğunda en çok oy alan harf
 * oynanır (eşitlikte önce oy alan). Oyuncu başına tek oy tutulur; her harf hamlesinden
 * ve sıra değişiminden sonra oylar silinir.
 */
@Entity
@Table(name = "hangman_team_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanTeamVote {

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

    /** Oy verilen harf (küçük harf). */
    @Column(nullable = false, length = 1)
    private String letter;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Takımın Adam Asmaca oyunu için eklediği özel kelime.
 * Dahili (built-in) kelime havuzuna ek olarak rastgele seçime dahil edilir.
 */
@Entity
@Table(name = "hangman_words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanWord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** "tr" | "en" */
    @Column(nullable = false, length = 2)
    private String language;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String createdByEmail;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Quiz oturumuna katılan kullanıcı.
 * Toplam puanı saklar.
 */
@Entity
@Table(name = "quiz_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession session;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column
    private String displayName;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer answeredCount = 0;
}


package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Bir katılımcının belirli bir soruya verdiği cevap.
 */
@Entity
@Table(name = "quiz_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "question_id", "user_email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    /**
     * Kullanıcının seçtiği seçenek index'i. -1 = süre doldu / cevap vermedi.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer selectedOptionIndex = -1;

    /**
     * Cevap verme süresi (milisaniye cinsinden).
     */
    @Column
    private Long answeredInMs;

    /**
     * Bu cevap için kazanılan puan.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0;

    /**
     * Doğru mu?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean correct = false;
}


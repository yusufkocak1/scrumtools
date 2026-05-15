package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aktif veya tamamlanmış bir quiz oturumu.
 * Bir QuizTemplate üzerinden başlatılır.
 */
@Entity
@Table(name = "quiz_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuizTemplate template;

    @Column(nullable = false)
    private String hostEmail;

    @Column
    private String hostName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuizSessionStatus status = QuizSessionStatus.LOBBY;

    /**
     * Şu anda gösterilen soru index'i (0-based). -1 = henüz başlamadı.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer currentQuestionIndex = -1;

    /**
     * Aktif sorunun başladığı zaman (ms cinsinden). Puan hesabı için kullanılır.
     */
    @Column
    private Long questionStartedAtMs;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuizParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuizAnswer> answers = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;
}


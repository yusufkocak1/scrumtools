package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Quiz şablonundaki bir soru.
 * Soru başına ayarlanabilir süre (timeLimitSeconds).
 * Seçenekler ElementCollection olarak tutulur.
 */
@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuizTemplate template;

    @Column(nullable = false)
    private Integer questionOrder;

    @Column(nullable = false, length = 1000)
    private String questionText;

    /**
     * Seçenekler — sıralı liste, index 0'dan başlar.
     */
    @ElementCollection
    @CollectionTable(name = "quiz_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text", length = 500)
    @OrderColumn(name = "option_order")
    @Builder.Default
    private List<String> options = new ArrayList<>();

    /**
     * Doğru cevabın index'i (0-based).
     */
    @Column(nullable = false)
    private Integer correctOptionIndex;

    /**
     * Bu soru için süre limiti (saniye). Soru başına ayarlanabilir.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer timeLimitSeconds = 20;

    /**
     * Bu soruya verilen cevaplar. Soru silindiğinde cevaplar da cascade ile silinir.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuizAnswer> answers = new ArrayList<>();
}


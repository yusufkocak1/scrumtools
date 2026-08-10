package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quiz sorularına eklenen görsel.
 *
 * Görsel, soru kaydından bağımsız tutulur: şablon formunda soru henüz kaydedilmeden
 * yüklenip önizlenebilsin diye. Soru yalnızca görselin kalıcı medya bağlantısını
 * ({@link com.scrumtools.service.MediaLinkService}) saklar.
 */
@Entity
@Table(name = "quiz_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 1000)
    private String objectKey;

    @Column
    private String fileName;

    @Column
    private String mimeType;

    @Column
    private Long fileSize;

    @Column(nullable = false)
    private String uploadedByEmail;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

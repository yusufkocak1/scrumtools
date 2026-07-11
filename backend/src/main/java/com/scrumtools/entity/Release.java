package com.scrumtools.entity;

import com.scrumtools.entity.enums.ReleaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Proje seviyesinde sürüm (release) — Jira fixVersion benzeri.
 * Task'lar release'e nullable FK ile bağlanır; RELEASED geçişinde
 * dağıtım tarihçesi ReleaseDeployment tablosuna snapshot'lanır.
 */
@Entity
@Table(name = "releases", indexes = {
        @Index(name = "idx_release_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Sürüm adı, örn. "v2.4.0" */
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Durum geçişlerini (onay, yayınlama) sadece bu kullanıcı ve org admin yapabilir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_manager_id")
    private User releaseManager;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ReleaseStatus status = ReleaseStatus.OPEN;

    /** Paket kapanış (code freeze) tarihi */
    private LocalDate freezeDate;

    /** Planlanan yayın tarihi */
    private LocalDate plannedReleaseDate;

    /** Gerçekleşen yayın anı (RELEASED geçişinde set edilir) */
    private LocalDateTime actualReleaseDate;

    /** Oluşturan kullanıcı email */
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

package com.scrumtools.entity;

import com.scrumtools.entity.enums.CiEnvironment;
import com.scrumtools.entity.enums.CiJobType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeye eşlenmiş CI job'ı. Bir projeye birden çok job eşlenebilir; hangi
 * ekranda görüneceğini {@link CiJobType}, hedefini {@link CiEnvironment} belirler
 * (ör. "test-1'e deploy" ve "test-2'ye deploy" ayrı iki eşlemedir).
 */
@Entity
@Table(name = "ci_job_mappings",
        uniqueConstraints = @UniqueConstraint(name = "uq_ci_mapping_conn_project_job",
                columnNames = {"connection_id", "project_id", "jobFullName"}),
        indexes = {
                @Index(name = "idx_ci_mapping_project", columnList = "project_id"),
                @Index(name = "idx_ci_mapping_connection", columnList = "connection_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiJobMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private CiConnection connection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Folder dahil tam yol: "scrumtools/deploy-test" */
    @Column(nullable = false)
    private String jobFullName;

    /** UI'da görünen ad: "Test ortamına deploy" */
    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CiJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CiEnvironment environment;

    /**
     * Tetikleme parametrelerinin JSON şablonu; değerlerdeki {{...}} yer tutucuları
     * CiParameterResolver bağlamdan çözer. Boş/null = parametresiz job.
     * Örn: {"BRANCH":"{{branch}}","ENV":"test"}
     */
    @Column(columnDefinition = "TEXT")
    private String parameterTemplate;

    /**
     * Yalnız RELEASE_PIPELINE için anlamlı: build SUCCESS olunca release otomatik
     * RELEASED'a geçsin mi. Varsayılan kapalı (temkinli — bkz. plan K5).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean autoTransitionOnSuccess = false;

    /** Eşlemeyi silmeden (tarihçeyi kaybetmeden) devre dışı bırakma */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

package com.scrumtools.entity;

import com.scrumtools.entity.enums.CustomFieldType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proje/takım bazlı özel alan tanımı.
 * Task'ların customFields JSONB bölümünde saklanan değerlerin
 * meta-bilgisini (tip, seçenekler, zorunluluk) tutar.
 */
@Entity
@Table(name = "custom_field_definitions", indexes = {
        @Index(name = "idx_cfd_team", columnList = "team_id"),
        @Index(name = "idx_cfd_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** İnsan okunabilir ad (ör: "Environment", "Fix Version") */
    @Column(nullable = false)
    private String name;

    /** customFields JSON key (ör: "environment", "fixVersion") */
    @Column(nullable = false)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomFieldType fieldType;

    /**
     * SELECT/MULTI_SELECT için seçenek listesi.
     * { "options": ["Production", "Staging", "Dev"] }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> options;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    /** Hangi issue type'larda gösterilsin. Boş = hepsinde. */
    @ElementCollection
    @CollectionTable(name = "custom_field_issue_types", joinColumns = @JoinColumn(name = "field_id"))
    @Column(name = "issue_type")
    @Builder.Default
    private List<String> issueTypes = new ArrayList<>();

    @Column
    private String defaultValue;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}


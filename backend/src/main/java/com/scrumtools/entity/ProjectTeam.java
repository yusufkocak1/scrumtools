package com.scrumtools.entity;

import com.scrumtools.entity.enums.MemberType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Projeye bağlanmış takım — takım üyeliği ile proje üyeliği arasındaki kalıcı bağ.
 * <p>
 * Bu bağ olmadan "takımı projeye ekle" yalnızca o anki üyeleri kopyalar; takıma
 * sonradan katılan kimse projeye düşmez. Bağ kayıtlı olduğu için takım üyeliğindeki
 * her değişiklik projeye yansıtılabilir ({@code ProjectTeamService}).
 * <p>
 * {@link #roles} ve {@link #memberType}, bağ üzerinden eklenen üyelere uygulanacak
 * varsayılanlardır — takıma yeni katılan kişi projeye bu ayarlarla eklenir.
 */
@Entity
@Table(name = "project_teams",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "team_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** Bağ üzerinden eklenen üyelere verilecek proje rolleri. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "project_team_roles",
            joinColumns = @JoinColumn(name = "project_team_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberType memberType = MemberType.MEMBER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private User addedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

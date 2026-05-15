package com.scrumtools.entity;

import com.scrumtools.entity.enums.MemberType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "project_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "project_member_roles",
            joinColumns = @JoinColumn(name = "project_member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private User addedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberType memberType = MemberType.MEMBER;

    /**
     * Kullanıcının tüm rollerinden toplanan izinleri döndürür.
     */
    public Set<com.scrumtools.entity.enums.Permission> getAllPermissions() {
        Set<com.scrumtools.entity.enums.Permission> allPerms = new HashSet<>();
        for (Role role : roles) {
            allPerms.addAll(role.getPermissions());
        }
        return allPerms;
    }

    /**
     * Kullanıcının belirli bir izne sahip olup olmadığını kontrol eder.
     */
    public boolean hasPermission(com.scrumtools.entity.enums.Permission permission) {
        return getAllPermissions().contains(com.scrumtools.entity.enums.Permission.ADMIN_FULL_ACCESS)
                || getAllPermissions().contains(permission);
    }
}


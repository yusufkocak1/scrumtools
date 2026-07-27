package com.scrumtools.entity;

import com.scrumtools.entity.enums.InvitationStatus;
import com.scrumtools.entity.enums.InvitationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationType type;

    @Column(nullable = false)
    private UUID targetId; // org/project/team id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    /**
     * Davet kabul edildiğinde üyenin ekleneceği takımlar (opsiyonel).
     * Takım projelere bağlıysa üye o projelere de otomatik düşer.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invitation_teams", joinColumns = @JoinColumn(name = "invitation_id"))
    @Column(name = "team_id")
    @Builder.Default
    private Set<UUID> teamIds = new LinkedHashSet<>();

    @Column(unique = true, nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private User invitedBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime acceptedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}


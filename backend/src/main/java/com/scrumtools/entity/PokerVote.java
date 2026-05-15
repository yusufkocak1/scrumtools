package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Bir takım oturumundaki kullanıcının poker oyu.
 * Kullanıcı oturuma katıldığında oluşturulur, ayrıldığında silinir.
 */
@Entity
@Table(name = "poker_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PokerVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column
    private String displayName;

    @Column(nullable = false)
    @Builder.Default
    private String vote = "-";

    @Column
    private Long timestamp;
}


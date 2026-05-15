package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Bir takıma ait ScrumPoker oturum durumu.
 * votesVisible: oylar gösteriliyor mu?
 * cardType: kullanılan kart seti (fibonacci, tshirt, vb.)
 */
@Entity
@Table(name = "poker_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PokerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @Column(nullable = false)
    @Builder.Default
    private Boolean votesVisible = false;

    @Column(nullable = false)
    @Builder.Default
    private String cardType = "fibonacci";
}


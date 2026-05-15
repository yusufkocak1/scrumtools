package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "retro_item_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"retro_item_id", "owner"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroItemVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retro_item_id", nullable = false)
    private RetroItem retroItem;

    /** Voter email */
    @Column(nullable = false)
    private String owner;

    /** +1 (upvote) veya -1 (downvote) */
    @Column(nullable = false)
    private int voteValue;
}


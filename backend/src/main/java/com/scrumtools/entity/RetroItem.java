package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "retro_items", indexes = {
        @Index(name = "idx_retro_item_board", columnList = "retro_board_id"),
        @Index(name = "idx_retro_item_column", columnList = "column_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retro_board_id", nullable = false)
    private RetroBoard retroBoard;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    /** ACCEPTED | REJECTED | CONFLICT | null (pending) */
    private String status;

    /** Discussion timer: absolute end instant (UTC). null = no timer set. */
    @Column(name = "discussion_ends_at")
    private Instant discussionEndsAt;

    /** Total planned duration of the current discussion in seconds (for progress display). */
    @Column(name = "discussion_duration_seconds")
    private Integer discussionDurationSeconds;

    /** Owner email (or "Anonymous") */
    @Column(nullable = false)
    private String owner;

    @OneToMany(mappedBy = "retroItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RetroItemComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "retroItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RetroItemVote> votes = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}


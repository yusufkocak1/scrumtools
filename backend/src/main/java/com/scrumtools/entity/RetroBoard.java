package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "retro_boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String retroBoardName;

    /**
     * Kolon sıralaması önemli — ordered list olarak tutulur.
     * Örn: ["Start", "Stop", "Continue"]
     */
    @ElementCollection
    @CollectionTable(name = "retro_board_columns", joinColumns = @JoinColumn(name = "retro_board_id"))
    @Column(name = "column_name")
    @OrderColumn(name = "column_order")
    @Builder.Default
    private List<String> columns = new ArrayList<>();

    @OneToMany(mappedBy = "retroBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RetroItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}


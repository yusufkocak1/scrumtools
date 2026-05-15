package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kullanıcının kişisel dashboard widget düzenini saklar.
 * layout  — vue-grid-layout uyumlu widget listesi (JSONB)
 * Örnek widget item: { id, type, teamId, x, y, w, h, title }
 */
@Entity
@Table(name = "user_dashboards",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Dashboard widget listesi ve pozisyonları.
     * [{ "id":"w1","type":"SUMMARY","teamId":"...","x":0,"y":0,"w":4,"h":2,"title":"Takım Özeti" }, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> layout = List.of();

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}


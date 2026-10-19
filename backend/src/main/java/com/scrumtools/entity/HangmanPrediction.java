package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sırası olmayan oyuncunun "sıradaki harf bu olacak" tahmini.
 *
 * Bekleyen oyuncular boş durmasın diye vardır: sırası gelen oyuncu bir harf denediğinde
 * aynı harfi tahmin etmiş olanlar — harf kelimede varsa — küçük bir bonus alır. Tahminler
 * gizlidir (yayında yalnızca sayıları görünür) ve her harf tahmininden sonra silinir;
 * oyuncu başına aynı anda tek tahmin tutulur.
 */
@Entity
@Table(name = "hangman_predictions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private HangmanSession session;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column
    private String displayName;

    /** Tahmin edilen harf (küçük harf). */
    @Column(nullable = false, length = 1)
    private String letter;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

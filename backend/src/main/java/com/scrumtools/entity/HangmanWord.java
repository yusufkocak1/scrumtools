package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adam Asmaca oyunu için sisteme eklenmiş kelime.
 * Sadece SUPER_ADMIN tarafından yönetilir; dahili (built-in) kelime havuzuna ek olarak
 * tüm takımlar için rastgele seçime dahil edilir (takım bazlı değil, global).
 */
@Entity
@Table(name = "hangman_words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanWord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** "tr" | "en" */
    @Column(nullable = false, length = 2)
    private String language;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String createdByEmail;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

package com.scrumtools.entity;

import com.scrumtools.entity.enums.ScmConnectionStatus;
import com.scrumtools.entity.enums.ScmProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanıcının kendi SCM hesabı (kişisel PAT). Opsiyoneldir;
 * branch açarken varsa bu token kullanılır (branch sağlayıcıda kullanıcının
 * adına açılır), yoksa org bağlantısının tokenına düşülür.
 * providerEmail commit yazarlarını uygulama kullanıcılarıyla eşlemek içindir.
 */
@Entity
@Table(name = "user_scm_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_scm_account",
                columnNames = {"user_id", "provider", "baseUrl"}),
        indexes = @Index(name = "idx_user_scm_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserScmAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScmProvider provider;

    /** Self-hosted eşlemesi için bağlantıyla aynı host olmalı; null = cloud */
    @Column
    private String baseUrl;

    /** AES-GCM şifreli PAT — base64(iv || ciphertext+tag) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedToken;

    @Column
    private String tokenHint;

    /** Sağlayıcıdaki kullanıcı adı (validateToken sırasında API'den çekilir) */
    @Column
    private String username;

    /** Commit yazarı eşleştirmede kullanılır (/user endpoint'inden) */
    @Column
    private String providerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScmConnectionStatus status = ScmConnectionStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

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
 * Org seviyesi SCM bağlantısı (servis hesabı / bot token mantığı).
 * Repo keşfi, webhook kaydı ve commit senkronizasyonu bu bağlantı üzerinden yapılır.
 * Token AES-GCM ile şifreli saklanır (ScmTokenCrypto) ve hiçbir API yanıtında yer almaz.
 */
@Entity
@Table(name = "scm_connections", indexes = {
        @Index(name = "idx_scm_conn_org", columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScmConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScmProvider provider;

    /** Görünen ad: "Şirket GitHub'ı" gibi */
    @Column(nullable = false)
    private String name;

    /** Self-hosted kurulum URL'i; null = sağlayıcının cloud varsayılanı */
    @Column
    private String baseUrl;

    /** AES-GCM şifreli PAT — base64(iv || ciphertext+tag) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedToken;

    /** Maskeli gösterim: ****abcd (son 4 karakter) */
    @Column
    private String tokenHint;

    /** Bitbucket Basic auth için zorunlu; diğer sağlayıcılarda opsiyonel */
    @Column
    private String username;

    /** Bağlantı başına random secret — webhook imza doğrulamada kullanılır */
    @Column(nullable = false)
    private String webhookSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScmConnectionStatus status = ScmConnectionStatus.ACTIVE;

    /** Bağlantıyı ekleyen org admin (email) */
    @Column
    private String createdBy;

    @Column
    private LocalDateTime lastValidatedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

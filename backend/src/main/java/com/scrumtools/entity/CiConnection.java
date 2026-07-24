package com.scrumtools.entity;

import com.scrumtools.entity.enums.CiConnectionStatus;
import com.scrumtools.entity.enums.CiProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Org seviyesi CI/CD bağlantısı (Jenkins servis hesabı mantığı).
 * Job keşfi ve build tetikleme bu bağlantı üzerinden yapılır.
 * Token AES-GCM ile şifreli saklanır (ScmTokenCrypto) ve hiçbir API yanıtında yer almaz.
 */
@Entity
@Table(name = "ci_connections", indexes = {
        @Index(name = "idx_ci_conn_org", columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CiProvider provider;

    /** Görünen ad: "Şirket Jenkins'i" gibi */
    @Column(nullable = false)
    private String name;

    /** Jenkins kök adresi — SCM'den farklı olarak zorunlu (cloud varsayılanı yok) */
    @Column(nullable = false)
    private String baseUrl;

    /** Jenkins kullanıcı adı — API token ile birlikte Basic auth oluşturur */
    @Column(nullable = false)
    private String username;

    /** AES-GCM şifreli API token — base64(iv || ciphertext+tag) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedToken;

    /** Maskeli gösterim: ****abcd (son 4 karakter) */
    @Column
    private String tokenHint;

    /** Bağlantı başına random secret — ileride webhook imza doğrulamada kullanılacak */
    @Column(nullable = false)
    private String webhookSecret;

    /**
     * Kurulum CSRF crumb istiyor mu — bağlantı testinde otomatik tespit edilip yazılır.
     * API token kullanımında çoğu kurulumda gerekmez, ama zorunlu kılan yapılandırmalar var.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean crumbEnabled = false;

    /** Test sırasında okunan X-Jenkins sürüm başlığı — teşhis için gösterilir */
    @Column
    private String serverVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CiConnectionStatus status = CiConnectionStatus.ACTIVE;

    /** Bağlantıyı ekleyen org admin (email) */
    @Column
    private String createdBy;

    @Column
    private LocalDateTime lastValidatedAt;

    /**
     * Poller'ın ardışık başarısız sorgu sayacı; eşiği aşınca bağlantı INVALID'e çekilir.
     * Başarılı her sorguda sıfırlanır.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

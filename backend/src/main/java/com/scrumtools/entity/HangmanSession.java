package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adam Asmaca takım oturumu.
 *
 * Oturumu başlatan kişi moderatördür (hostEmail). Turlar sırayla oynanır:
 * ortak bir kelime herkese gösterilir, sırası gelen oyuncu ya tek harf ya da
 * kelimenin tamamını tahmin eder.
 *
 * ÖNEMLİ: Kelimenin kendisi bu tabloda (HangmanRound.word) tutulur ve
 * istemciye ASLA açık gönderilmez — sadece maskelenmiş hali gider.
 */
@Entity
@Table(name = "hangman_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String hostEmail;

    @Column
    private String hostName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HangmanSessionStatus status = HangmanSessionStatus.LOBBY;

    /** "tr" | "en" */
    @Column(nullable = false, length = 2)
    @Builder.Default
    private String language = "tr";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HangmanWordSource wordSource = HangmanWordSource.RANDOM;

    /**
     * Rastgele kelimelerin çekildiği kategori. null = tüm kategoriler karışık.
     * wordSource = CUSTOM ise anlamsızdır ve her zaman null'dır.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private HangmanCategory category;

    /**
     * Moderatör de oyuncu olarak sıraya giriyor mu?
     * wordSource = CUSTOM ise adalet gereği her zaman false'tur.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean moderatorPlays = true;

    /**
     * Oyun modu — sıralı, yarış ya da takım.
     * Kolon sonradan eklendi; eski oturumlarda null'dır, {@link HangmanMode#of} ile okuyun.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private HangmanMode mode;

    /** Takım modunda sırası gelen takım (1 ya da 2); oyun başlamadıysa ya da başka moddaysa null. */
    @Column
    private Integer currentTeam;

    /** Takım modunda takımların puanları (1 = Mavi, 2 = Kırmızı). Diğer modlarda kullanılmaz. */
    @Column
    @Builder.Default
    private Integer teamOneScore = 0;

    @Column
    @Builder.Default
    private Integer teamTwoScore = 0;

    /**
     * Oyun temposu — sıra süresini ve kelime kilidini belirler.
     * Kolon sonradan eklendi; eski oturumlarda null'dır, {@link HangmanPace#of} ile okuyun.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private HangmanPace pace;

    /**
     * Tur bitince ara ekranı kısa bir geri sayımdan sonra kendiliğinden kapansın mı?
     * Eski oturumlarda null'dır ve kapalı sayılır — {@code Boolean.TRUE.equals(...)} ile okuyun.
     */
    @Column
    private Boolean autoAdvance;

    /**
     * Otomatik geçişte ara ekranının kapanacağı an. Geri sayım işlemiyorsa null:
     * otomatik geçiş kapalı, tur arasında değil ya da moderatör duraklattı.
     */
    @Column
    private LocalDateTime intermissionEndsAt;

    /** Şu an oynanan tur index'i (0-based). -1 = henüz başlamadı. */
    @Column(nullable = false)
    @Builder.Default
    private Integer currentRoundIndex = -1;

    /** Sırası gelen oyuncunun e-postası. Oyun başlamadıysa ya da sıra bir oyuncuda değilse (yarış/takım) null. */
    @Column
    private String currentTurnEmail;

    /**
     * Mevcut sıranın başladığı an — süre sayacının referansı.
     *
     * İki kuralı birden besler:
     *   • Sıra süresi ({@link HangmanPace}) sonunda tahmin gelmezse sıra kendiliğinden devreder.
     *   • Yarış modunda sıra yoktur; bu alan turun (kelimenin) başladığı anı tutar ve
     *     yarış süresi dolunca tur kapanır.
     *
     * Yalnızca sıra devrettiğinde sıfırlanır — doğru harfle sırayı koruyan oyuncunun süresi
     * kaldığı yerden işler, böylece seri yapan biri sırayı süresiz elinde tutamaz.
     * Oyun başlamadıysa / bittiyse null.
     */
    @Column
    private LocalDateTime turnStartedAt;

    /**
     * Tur bitti, sonraki kelimeye geçmek için moderatörün onayı bekleniyor mu?
     *
     * Tur kapandığında bulunan kelime herkese ilan edilir ve oyun bu ara ekranında bekler:
     * otomatik geçiş açıksa {@code intermissionEndsAt} anına kadar, değilse (ya da moderatör
     * duraklattıysa) moderatör "Sonraki Kelime"ye basana kadar.
     * Bu sırada {@code currentRoundIndex} biten turu gösterir (cevabı açık gelsin diye),
     * {@code currentTurnEmail} ise turu bitiren oyuncuda durur — sonraki tur onun
     * sonrasındaki oyuncuyla başlasın diye. Süre sayaçları ara boyunca işlemez.
     *
     * Eski oturumlarda null olabilir; her zaman {@code Boolean.TRUE.equals(...)} ile okuyun.
     */
    @Column
    @Builder.Default
    private Boolean awaitingNextRound = false;

    // Turlar bilinçli olarak @OneToMany ile bağlanmadı: HangmanRound kayıtları
    // repository üzerinden yazılıyor, bu yüzden buradaki koleksiyon bayat kalırdı.
    // Turlara her zaman HangmanRoundRepository üzerinden erişin.

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;
}

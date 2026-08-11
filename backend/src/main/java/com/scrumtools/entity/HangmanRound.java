package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Oturumdaki tek bir kelime turu.
 *
 * GÜVENLİK: {@code word} gizli cevaptır. DTO katmanı bu alanı yalnızca tur
 * bittiğinde (SOLVED/FAILED) dışarı verir; oynanırken sadece maskelenmiş hali gider.
 */
@Entity
@Table(name = "hangman_rounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HangmanRound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private HangmanSession session;

    @Column(nullable = false)
    private Integer roundOrder;

    /** Gizli cevap — küçük harf, boşluksuz. */
    @Column(nullable = false)
    private String word;

    /**
     * Bu turdaki kelimenin kategorisi — moderatörün ipucu olarak açabileceği bilgi.
     * Rastgele kelimede kelimenin geldiği kategori, moderatörün kendi kelimelerinde
     * kurulumda seçtiği ipucu kategorisidir. Bilinmiyorsa null (ipucu verilemez).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private HangmanCategory category;

    /**
     * Kategori oyunculara gösterildi mi? Oturum sabit bir kategoriyle açıldıysa kategori
     * zaten lobide herkese yazdığı için baştan true'dur; aksi hâlde moderatör açana kadar false.
     *
     * Bilinçli olarak nullable: ddl-auto:update mevcut satırlara NOT NULL kolon ekleyemez,
     * bu yüzden okurken her zaman Boolean.TRUE.equals(...) ile değerlendirin.
     */
    @Column
    @Builder.Default
    private Boolean categoryRevealed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HangmanRoundStatus status = HangmanRoundStatus.PENDING;

    /** Bu turda denenmiş harfler (doğru + yanlış), küçük harf. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hangman_round_letters", joinColumns = @JoinColumn(name = "round_id"))
    @Column(name = "letter", length = 4)
    @Builder.Default
    private List<String> guessedLetters = new ArrayList<>();

    /** Yanlış harf sayısı — adamın kaç parçasının çizildiğini belirler. */
    @Column(nullable = false)
    @Builder.Default
    private Integer wrongCount = 0;

    /** Kelimeyi bitiren oyuncu (harfi tamamlayan ya da kelimeyi bilen). */
    @Column
    private String solvedByEmail;

    @Column
    private String solvedByName;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;
}

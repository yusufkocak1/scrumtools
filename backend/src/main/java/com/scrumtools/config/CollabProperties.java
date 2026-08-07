package com.scrumtools.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ortak çalışma alanı ayarları (COLLAB_WORKSPACE_PLAN.md §12 kaynak bütçesi).
 *
 * <p>Varsayılanlar dar sunucuya (plan D3: Redis yok, tek örnek) göre seçilmiştir;
 * yükseltmeden önce §12'deki izleme kalemlerine bakılmalıdır.
 */
@Component
@ConfigurationProperties(prefix = "app.collab")
@Getter
@Setter
public class CollabProperties {

    /**
     * Plan K9 — tek backend örneği kuralı.
     *
     * <p>Broker süreç-içidir ({@code enableSimpleBroker}) ve ortak çalışma
     * oturumları bellekte tutulur. İkinci bir örnek açılırsa A'ya bağlı kullanıcı
     * B'ye bağlı kullanıcının düzenlemesini görmez — sessiz veri ayrışması.
     * {@code false} yapmak ölçeklemeyi <b>mümkün kılmaz</b>, yalnızca açılıştaki
     * uyarıyı susturur; yatay ölçek için önce doküman yapışkanlığı gerekir.
     */
    private boolean singleInstance = true;

    /**
     * {@code /ws/collab} üzerinden kabul edilen tek ikili çerçevenin üst sınırı.
     * İlk senkron paketi (sync step 2) büyük bir dokümanda en büyük mesajdır.
     */
    private int maxBinaryMessageSize = 1024 * 1024;

    /**
     * Sunucu tarafı append gruplama penceresi (§12 madde 2). Aynı dokümana gelen
     * güncellemeler bu pencerede birleştirilip tek {@code INSERT}'e döner.
     * Röle bundan bağımsız ve anında yapılır — gecikmeyi artırmaz, disk yazımını
     * seyreltir.
     */
    private long appendBatchWindowMs = 1000;

    /**
     * FREE pakette organizasyon başına arşivlenmemiş doküman sayısı (plan §11).
     * Bu yalnızca bir satış sınırı değil, aynı zamanda D3'ün dar sunucusunu
     * ücretsiz hesapların doldurmasına karşı koruma.
     */
    private int freeMaxDocuments = 3;

    /** FREE pakette bir dokümanda aynı anda düzenleyebilecek kişi sayısı (plan §11). */
    private int freeMaxConcurrentUsers = 3;

    /**
     * Sıkıştırma eşiği: anlık görüntüden bu kadar güncelleme sonra doküman
     * budanmaya aday olur (§5). Yoksa uzun ömürlü doküman diski büyütür (R3).
     */
    private int compactionUpdateThreshold = 500;

    /**
     * Geçmişe yeni bir durak eklemek için iki anlık görüntü arasında geçmesi
     * gereken en kısa süre (ms). Anlık görüntü dakikada birkaç kez üretilir;
     * hepsini kaydetmek zaman çizelgesini okunamaz, diski de gereksiz dolu yapar.
     */
    private long historyMinIntervalMs = 10 * 60_000;

    /** Doküman başına saklanan durak sayısı; fazlası en eskiden silinir. */
    private int historyMaxEntries = 20;
}

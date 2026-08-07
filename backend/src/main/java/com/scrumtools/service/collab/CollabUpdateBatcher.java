package com.scrumtools.service.collab;

import com.scrumtools.config.CollabProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sunucu tarafı append gruplama (COLLAB_WORKSPACE_PLAN.md §12, madde 2).
 *
 * <p>Aktif bir düzenleme oturumunda saniyede onlarca küçük CRDT deltası akar.
 * Her birini ayrı {@code INSERT} yapmak dar sunucuda (plan D3) asıl yükün
 * Postgres'e binmesi demektir. Bu sınıf aynı dokümana gelen güncellemeleri bir
 * pencerede biriktirip tek yazmaya indirger.
 *
 * <p><b>Gecikmeyi artırmaz:</b> röle bu sınıftan tamamen bağımsızdır ve anında
 * yapılır (plan K2 — sunucu aptal röle). Burada seyrelen yalnızca <i>disk
 * yazımı</i>dır.
 *
 * <p><b>Faz 0 kapsamı:</b> gruplama ve tampon kontrolü. Yazan taraf
 * {@link CollabUpdateSink} arayüzünün arkasındadır; gerçek uygulaması
 * {@code collab_updates} tablosuyla Faz 1'de gelir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabUpdateBatcher {

    /**
     * Tüm dokümanlar için bekleyen toplam bayt üst sınırı. Aşılırsa pencerenin
     * dolması beklenmeden yazılır: sink yavaşladığında tamponun sınırsız büyüyüp
     * heap'i yemesi, §12'nin önlemeye çalıştığı şeyin ta kendisi olurdu.
     */
    private static final long MAX_BUFFERED_BYTES = 8L * 1024 * 1024;

    private final CollabProperties properties;
    private final CollabUpdateSink sink;

    private final ConcurrentHashMap<UUID, List<CollabUpdateSink.PendingUpdate>> pending =
            new ConcurrentHashMap<>();
    private final AtomicLong bufferedBytes = new AtomicLong();

    /**
     * İzleme sayacı (§12): uygulama açıldığından beri kabul edilen güncelleme
     * sayısı. Oran hesabı burada değil ekranda yapılıyor — iki ölçüm arasındaki
     * farkı almak, sunucuda kayan pencere tutmaktan hem ucuz hem doğru.
     */
    private final AtomicLong acceptedUpdates = new AtomicLong();

    /**
     * Röle yapıldıktan <b>sonra</b> çağrılır — kalıcılaştırma yayını hiçbir zaman
     * geciktirmemelidir.
     */
    public void enqueue(UUID documentId, String authorEmail, byte[] payload) {
        if (documentId == null || payload == null || payload.length == 0) {
            return;
        }
        acceptedUpdates.incrementAndGet();
        var update = new CollabUpdateSink.PendingUpdate(authorEmail, payload, Instant.now());

        // compute/computeIfPresent çifti anahtar bazında atomiktir; düz
        // computeIfAbsent + remove kullanılsaydı, flush listeyi haritadan
        // çıkardıktan sonra araya giren bir enqueue öksüz listeye yazar ve
        // güncelleme sessizce kaybolurdu.
        pending.compute(documentId, (id, updates) -> {
            if (updates == null) updates = new ArrayList<>();
            updates.add(update);
            return updates;
        });

        if (bufferedBytes.addAndGet(payload.length) > MAX_BUFFERED_BYTES) {
            log.warn("Collab append tamponu {} baytı aştı — pencere beklenmeden yazılıyor.",
                    MAX_BUFFERED_BYTES);
            flush();
        }
    }

    /**
     * {@code synchronized}, çünkü üç ayrı yoldan çağrılır: zamanlanmış görev,
     * tampon taşması (WS iş parçacığı) ve {@code GET /state} (HTTP iş parçacığı).
     * Sıra numarası ayırma okuyup-artır olduğundan, iki yol aynı dokümanda
     * çakışırsa aynı {@code seq} iki kez üretilir ve tekillik kısıtı patlar.
     */
    @Scheduled(fixedDelayString = "${app.collab.append-batch-window-ms:1000}")
    public synchronized void flush() {
        for (UUID documentId : List.copyOf(pending.keySet())) {
            flushDocument(documentId);
        }
    }

    /**
     * Tek dokümanı hemen yazar.
     *
     * <p>{@code GET /state} bunu çağırmak zorunda: aksi hâlde açılan istemci,
     * gruplama penceresi içinde yazılmış (henüz DB'de olmayan) düzenlemeleri
     * göremez. Yazan istemci o sırada bağlantıyı kapatmışsa o değişiklikler
     * hiçbir yerden gelmez.
     */
    public synchronized void flushDocument(UUID documentId) {
        List<CollabUpdateSink.PendingUpdate> batch = drain(documentId);
        if (batch.isEmpty()) return;
        try {
            sink.persist(documentId, batch);
        } catch (Exception e) {
            // Yayın zaten yapıldı, istemcilerdeki içerik doğru; kaybolan
            // yalnızca kurtarma log'unun bu parçası. Sessiz kalmamalı.
            log.error("Collab güncellemeleri kalıcılaştırılamadı: documentId={}, {} paket",
                    documentId, batch.size(), e);
        }
    }

    /** Kapanışta son pencereyi yazar — deploy sırasında 1 sn'lik yazım kaybolmasın. */
    @PreDestroy
    public synchronized void flushOnShutdown() {
        log.info("Collab: kapanış öncesi bekleyen güncellemeler yazılıyor.");
        flush();
    }

    private List<CollabUpdateSink.PendingUpdate> drain(UUID documentId) {
        var drained = new ArrayList<CollabUpdateSink.PendingUpdate>();
        pending.computeIfPresent(documentId, (id, updates) -> {
            drained.addAll(updates);
            return null; // eşlemeyi atomik olarak kaldırır
        });
        long freed = drained.stream().mapToLong(u -> u.payload().length).sum();
        bufferedBytes.addAndGet(-freed);
        return drained;
    }

    /** İzleme için (§12): açılıştan beri kabul edilen toplam güncelleme sayısı. */
    public long acceptedUpdates() {
        return acceptedUpdates.get();
    }

    /** İzleme için (§12): o an kalıcılaştırılmayı bekleyen bayt miktarı. */
    public long bufferedBytes() {
        return bufferedBytes.get();
    }

    /** Yapılandırılmış pencere — izleme ekranı ve testler için. */
    public long batchWindowMs() {
        return properties.getAppendBatchWindowMs();
    }
}

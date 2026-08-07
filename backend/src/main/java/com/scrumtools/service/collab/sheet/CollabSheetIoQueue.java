package com.scrumtools.service.collab.sheet;

import com.scrumtools.config.CollabProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * Excel işlerinin tek iş parçacıklı kuyruğu (COLLAB_WORKSPACE_PLAN.md §10).
 *
 * <p>Amaç eşzamanlılığı artırmak değil <b>sınırlamak</b>: D3'ün dar sunucusunda
 * iki büyük {@code .xlsx} aynı anda ayrıştırılırsa POI'nin bellek piki OOM
 * Killer'ı tetikler. İstekler sıraya girer, her seferinde tek dosya işlenir.
 *
 * <p>Kuyruk sınırlıdır: dolduğunda istek beklemeye alınmaz, hemen reddedilir.
 * Sınırsız kuyruk, kullanıcıya dakikalarca "yükleniyor" gösterip sonunda zaman
 * aşımı vermek demekti.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabSheetIoQueue {

    private static final int MAX_QUEUED = 8;

    private final CollabProperties properties;

    private final ExecutorService executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED),
            runnable -> {
                Thread thread = new Thread(runnable, "collab-sheet-io");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    /**
     * İşi kuyruğa alır ve sonucunu bekler.
     *
     * <p>Çağıran HTTP iş parçacığı bloke olur — bu bilinçli: dosya indirme/yükleme
     * zaten senkron bir kullanıcı eylemi, ayrıca bir iş takip mekanizması kurmak
     * Faz 3'ün kapsamını gereksiz büyütürdü.
     */
    public <T> T submit(String description, Callable<T> work) {
        Future<T> future;
        try {
            future = executor.submit(work);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException(
                    "Şu an başka hesap tablosu dosyaları işleniyor, birazdan tekrar deneyin.");
        }

        try {
            return future.get(properties.getSheetIoTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Hesap tablosu işi zaman aşımına uğradı: {}", description);
            throw new IllegalStateException("Dosya çok büyük ya da işlem çok uzun sürdü.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("İşlem yarıda kesildi.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("Hesap tablosu işi başarısız: {} — {}", description, cause.toString());
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Dosya işlenemedi: " + cause.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

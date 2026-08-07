package com.scrumtools.service.collab.macro;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Sunucu tarafı makroların <b>tek iş parçacıklı</b> kuyruğu
 * (COLLAB_WORKSPACE_PLAN.md K8 / D3 notu).
 *
 * <p>{@link com.scrumtools.service.collab.sheet.CollabSheetIoQueue} ile aynı
 * gerekçe, farklı kaynak: orada bellek piki, burada <b>CPU</b>. D3'ün dar
 * sunucusunda iki makro aynı anda koşarsa tek çekirdek paylaşılır ve ikisi de
 * bekçinin zaman aşımına yaklaşır; üçüncüsü tüm uygulamayı yavaşlatır. Amaç
 * eşzamanlılığı artırmak değil <b>bire sabitlemek</b>.
 *
 * <p>Kuyruk sınırlı ve <b>kısa</b>: dolduğunda istek beklemeye alınmaz, hemen
 * reddedilir. Uzun kuyruk, tetikleyiciyi dakikalarca bekletip sonunda zaman
 * aşımı vermek demekti — webhook gönderen sistem için en kötü cevap budur.
 *
 * <p><b>Güvenlik notu:</b> iş parçacığı yeniden kullanılıyor ve
 * {@code SecurityContextHolder} varsayılan olarak {@code ThreadLocal} tabanlı.
 * Buraya iş gönderen her yol, çalıştırma bittiğinde bağlamı temizlemek
 * <i>zorunda</i> — aksi hâlde bir makronun kimliği sıradaki makroya sızar.
 * Temizlik {@link ServerMacroRunner} içinde {@code finally} ile yapılıyor.
 */
@Component
@Slf4j
public class CollabMacroQueue {

    private static final int MAX_QUEUED = 4;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED),
            runnable -> {
                Thread thread = new Thread(runnable, "collab-macro");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    /**
     * İşi kuyruğa alır ve sonucunu bekler.
     *
     * @param totalTimeoutMs kuyrukta bekleme <b>dahil</b> üst sınır. Bekçinin
     *                       yürütme zaman aşımından uzun olmalı: aksi hâlde
     *                       sırada bekleyen bir makro hiç başlamadan
     *                       "zaman aşımı" damgası yerdi.
     */
    public <T> T submit(String description, long totalTimeoutMs, Callable<T> work) {
        Future<T> future;
        try {
            future = executor.submit(work);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException(
                    "Şu an başka makrolar çalışıyor; birazdan tekrar deneyin.");
        }

        try {
            return future.get(totalTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            log.warn("Sunucu makrosu zaman aşımına uğradı: {}", description);
            throw new IllegalStateException("Makro zaman aşımına uğradı.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Makro yarıda kesildi.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Makro çalıştırılamadı: " + cause.getMessage());
        }
    }

    /** İzleme (§12): o an kuyrukta bekleyen makro sayısı. */
    public int queueDepth() {
        return executor.getQueue().size();
    }

    /** İzleme (§12): şu an bir makro koşuyor mu. */
    public int activeCount() {
        return executor.getActiveCount();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

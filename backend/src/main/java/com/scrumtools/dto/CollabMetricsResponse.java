package com.scrumtools.dto;

import java.util.List;
import java.util.UUID;

/**
 * Ortak çalışma alanı kaynak göstergeleri (COLLAB_WORKSPACE_PLAN.md §12).
 *
 * <p>§12'nin cümlesi şu: "kaynak tükenmeden görmek gerekir". Buradaki kalemler
 * o cümleyi karşılayacak kadar — tam bir metrik altyapısı değil. D3 gereği
 * Prometheus/Grafana gibi ek servis kurulmuyor; sayılar bellekten ve iki
 * sorgudan geliyor.
 *
 * @param openConnections     o an açık {@code /ws/collab} bağlantısı
 * @param openDocuments       en az bir bağlantısı olan doküman sayısı
 * @param acceptedUpdates     açılıştan beri kabul edilen güncelleme sayısı;
 *                            oran, iki ölçüm arasındaki farktan hesaplanır
 * @param bufferedBytes       kalıcılaştırılmayı bekleyen bayt (append tamponu)
 * @param totalUpdateRows     {@code collab_updates} toplam satır sayısı
 * @param macroQueueDepth     sunucu makro kuyruğunda bekleyen iş
 * @param macroRunning        o an koşan sunucu makrosu (0 ya da 1)
 * @param heapUsedBytes       JVM heap kullanımı — POI ve makro piklerinin izi
 * @param topDocuments        sıkıştırılmamış yükü en büyük 10 doküman (R3)
 */
public record CollabMetricsResponse(
        int openConnections,
        int openDocuments,
        long acceptedUpdates,
        long bufferedBytes,
        long totalUpdateRows,
        int macroQueueDepth,
        int macroRunning,
        long heapUsedBytes,
        long heapMaxBytes,
        List<TopDocument> topDocuments
) {
    public record TopDocument(UUID documentId, String title, long updateCount, long payloadBytes) {
    }
}

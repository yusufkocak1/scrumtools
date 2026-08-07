package com.scrumtools.websocket;

import com.scrumtools.dto.CollabParticipantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Açık ortak çalışma oturumlarının bellek-içi kaydı (COLLAB_WORKSPACE_PLAN.md K6/K9).
 *
 * <p><b>Neden bellekte:</b> Redis yok (plan D3) ve backend tek örnek (K9). Bu bilinçli
 * bir takas: yeniden başlatmada tüm oturumlar düşer ama <b>veri kaybolmaz</b> —
 * içerik {@code collab_updates} append log'undadır, istemciler geri bağlanıp
 * senkronlanır.
 *
 * <p><b>Yazar seçimi (K6):</b> sunucuda Yjs olmadığı için anlık görüntüyü bir
 * istemci üretmek zorunda. Karışıklık olmasın diye doküman başına tek "yazar"
 * seçilir: <i>yazma yetkisi olan, en eski bağlanan</i> oturum. Yazar düşünce
 * yerine yenisi atanır ve herkese duyurulur.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabSessionRegistry {

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<UUID, CopyOnWriteArrayList<CollabSession>> sessionsByDocument =
            new ConcurrentHashMap<>();

    /** Kim yazar — doküman → oturum kimliği. */
    private final Map<UUID, String> writerByDocument = new ConcurrentHashMap<>();

    /**
     * Oturumu kaydeder.
     *
     * @return kota nedeniyle reddedilirse {@code false}
     */
    public boolean join(CollabSession session, int concurrentUserLimit) {
        var sessions = sessionsByDocument.computeIfAbsent(
                session.getDocumentId(), id -> new CopyOnWriteArrayList<>());

        // Kota kişi bazlıdır, sekme bazlı değil: aynı kullanıcının iki sekmesi
        // tek kişi sayılır, aksi hâlde FREE kullanıcı kendi kendini kilitlerdi.
        long distinctUsers = sessions.stream()
                .map(CollabSession::getEmail)
                .filter(email -> !email.equalsIgnoreCase(session.getEmail()))
                .distinct()
                .count();
        if (distinctUsers >= concurrentUserLimit) {
            log.info("Collab eşzamanlı kullanıcı kotası doldu: documentId={}, limit={}",
                    session.getDocumentId(), concurrentUserLimit);
            return false;
        }

        sessions.add(session);
        electWriter(session.getDocumentId());
        broadcastPresence(session.getDocumentId());
        return true;
    }

    public void leave(CollabSession session) {
        UUID documentId = session.getDocumentId();
        var sessions = sessionsByDocument.get(documentId);
        if (sessions == null) return;

        sessions.remove(session);
        if (sessions.isEmpty()) {
            // Boş liste bırakılırsa harita açılan her dokümanla birlikte sızar.
            sessionsByDocument.remove(documentId, sessions);
            writerByDocument.remove(documentId);
            return;
        }
        electWriter(documentId);
        broadcastPresence(documentId);
    }

    /** Röle hedefleri: aynı dokümandaki diğer oturumlar. */
    public List<CollabSession> peers(CollabSession sender) {
        return sessionsByDocument.getOrDefault(sender.getDocumentId(), new CopyOnWriteArrayList<>())
                .stream()
                .filter(s -> !s.getId().equals(sender.getId()))
                .toList();
    }

    public boolean isWriter(CollabSession session) {
        return session.getId().equals(writerByDocument.get(session.getDocumentId()));
    }

    public List<CollabParticipantResponse> participants(UUID documentId) {
        return sessionsByDocument.getOrDefault(documentId, new CopyOnWriteArrayList<>())
                .stream()
                .map(s -> new CollabParticipantResponse(
                        s.getEmail(), s.getDisplayName(), s.getColor(), s.isCanWrite()))
                .distinct()
                .toList();
    }

    /** İzleme (§12): o an açık toplam bağlantı sayısı. */
    public int openConnectionCount() {
        return sessionsByDocument.values().stream().mapToInt(List::size).sum();
    }

    public int openDocumentCount() {
        return sessionsByDocument.size();
    }

    // ─── Yazar seçimi ────────────────────────────────────────────────────────

    /**
     * Yazma yetkisi olan en eski oturumu yazar yapar.
     *
     * <p>Salt-okunur bir oturum yazar seçilemez: anlık görüntü ucu yazma yetkisi
     * ister, seçilseydi doküman hiç kaydedilmezdi.
     */
    private void electWriter(UUID documentId) {
        var sessions = sessionsByDocument.get(documentId);
        if (sessions == null || sessions.isEmpty()) {
            writerByDocument.remove(documentId);
            return;
        }

        Optional<CollabSession> candidate = sessions.stream()
                .filter(CollabSession::isCanWrite)
                .min(Comparator.comparingLong(CollabSession::getConnectedAt));

        String previous = writerByDocument.get(documentId);
        String next = candidate.map(CollabSession::getId).orElse(null);

        if (next == null) {
            writerByDocument.remove(documentId);
        } else {
            writerByDocument.put(documentId, next);
        }
        if (java.util.Objects.equals(previous, next)) return;

        candidate.ifPresent(session -> session.send(
                CollabControlMessages.writerAssigned(true)));
        sessions.stream()
                .filter(s -> s.getId().equals(previous))
                .findFirst()
                .ifPresent(s -> s.send(CollabControlMessages.writerAssigned(false)));

        // STOMP tarafı: doküman açık olmayan ekranlar da (Faz 2'deki Docs rozeti)
        // kimin kaydedeceğini bilsin.
        publish(documentId, "writer", Map.of("writerSessionId", next == null ? "" : next));
        log.debug("Collab yazarı atandı: documentId={}, sessionId={}", documentId, next);
    }

    private void broadcastPresence(UUID documentId) {
        publish(documentId, "presence", participants(documentId));
    }

    /** "Kaydedildi" bildirimi — anlık görüntü ucundan çağrılır. */
    public void publishSaved(UUID documentId, Object payload) {
        publish(documentId, "saved", payload);
    }

    /**
     * Yazardan anlık görüntü ister.
     *
     * @return istek gönderilebildiyse {@code true}; doküman açık değilse ya da
     *         açık olanların hiçbirinde yazma yetkisi yoksa {@code false}
     */
    public boolean requestSnapshot(UUID documentId) {
        String writerId = writerByDocument.get(documentId);
        if (writerId == null) return false;
        return sessionsByDocument.getOrDefault(documentId, new CopyOnWriteArrayList<>())
                .stream()
                .filter(s -> s.getId().equals(writerId))
                .findFirst()
                .map(s -> {
                    s.send(CollabControlMessages.snapshotRequested());
                    return true;
                })
                .orElse(false);
    }

    private void publish(UUID documentId, String channel, Object payload) {
        try {
            messagingTemplate.convertAndSend("/topic/collab/" + documentId + "/" + channel, payload);
        } catch (Exception e) {
            // STOMP yayını yardımcı bir kanaldır; düşerse ortak düzenleme
            // (ham WS) çalışmaya devam etmeli.
            log.debug("Collab STOMP yayını başarısız: {}/{} — {}", documentId, channel, e.toString());
        }
    }
}

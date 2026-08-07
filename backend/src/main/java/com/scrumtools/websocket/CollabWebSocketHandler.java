package com.scrumtools.websocket;

import com.scrumtools.config.CollabProperties;
import com.scrumtools.repository.CollabUpdateRepository;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.collab.CollabDocumentService;
import com.scrumtools.service.collab.CollabUpdateBatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ortak çalışma alanının ham WebSocket rölesi (COLLAB_WORKSPACE_PLAN.md K2/K3).
 *
 * <p>Sunucunun yaptığı üç şey: <b>yetkiyi doğrula, aynen ilet, kalıcılaştır.</b>
 * CRDT açılmaz, yorumlanmaz, bellekte tutulmaz — birleştirme istemcide,
 * matematiksel olarak çakışmasız yapılır. Bu bir zarafet tercihi değil kaynak
 * kararıdır (plan D3): hesabı istemciler yapar, sunucu yalnızca bayt taşır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabWebSocketHandler extends BinaryWebSocketHandler {

    /** Yavaş istemci için biriktirme sınırı; aşılırsa oturum kapatılır. */
    private static final int SEND_BUFFER_LIMIT = 512 * 1024;
    private static final int SEND_TIME_LIMIT_MS = 10_000;

    private final CollabProperties properties;
    private final CollabSessionRegistry registry;
    private final CollabDocumentService documentService;
    private final CollabUpdateBatcher batcher;
    private final CollabUpdateRepository updateRepository;
    private final UserRepository userRepository;

    private final Map<String, CollabSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) throws Exception {
        String denial = (String) rawSession.getAttributes().get(CollabHandshakeInterceptor.ATTR_DENIAL);
        if (denial != null) {
            log.debug("Collab bağlantısı reddedildi ({}): sessionId={}", denial, rawSession.getId());
            rawSession.close(new CloseStatus(CollabProtocol.CLOSE_FORBIDDEN, denial));
            return;
        }

        rawSession.setBinaryMessageSizeLimit(properties.getMaxBinaryMessageSize());

        // Röle, mesajı üreten istemcinin iş parçacığından yazar; aynı hedefe iki
        // farklı iş parçacığından eşzamanlı gönderim Spring oturumlarında güvenli
        // değildir (çerçeveler iç içe geçip protokolü bozar).
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(
                rawSession, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT);

        UUID documentId = attr(rawSession, CollabHandshakeInterceptor.ATTR_DOCUMENT_ID);
        String email = attr(rawSession, CollabHandshakeInterceptor.ATTR_EMAIL);
        boolean canWrite = Boolean.TRUE.equals(
                rawSession.getAttributes().get(CollabHandshakeInterceptor.ATTR_CAN_WRITE));

        CollabSession collabSession = new CollabSession(
                session, documentId, email, displayName(email),
                CollabColors.forEmail(email), canWrite);

        if (!registry.join(collabSession, documentService.concurrentUserLimit(documentId))) {
            rawSession.close(new CloseStatus(CollabProtocol.CLOSE_QUOTA_EXCEEDED,
                    "concurrent-user-limit"));
            return;
        }
        sessions.put(rawSession.getId(), collabSession);

        collabSession.send(CollabControlMessages.hello(
                email, collabSession.getDisplayName(), collabSession.getColor(),
                canWrite, registry.isWriter(collabSession)));

        replayMissedUpdates(collabSession,
                attr(rawSession, CollabHandshakeInterceptor.ATTR_SINCE));

        log.debug("Collab bağlantısı açıldı: documentId={}, user={}, canWrite={}",
                documentId, email, canWrite);
    }

    /**
     * {@code GET /state} ile bu bağlantı arasındaki boşlukta yazılanları gönderir.
     *
     * <p>Önce gruplama tamponu boşaltılır, sonra kayıt sorgulanır. Bu sırada röle
     * de çalıştığı için istemci bazı paketleri iki kez alabilir — Yjs güncellemeleri
     * etkisiz-tekrarlanabilir olduğundan bu zararsızdır ve kaçırmaktan iyidir.
     */
    private void replayMissedUpdates(CollabSession session, Long since) {
        long from = since != null ? since : 0L;
        batcher.flushDocument(session.getDocumentId());

        var missed = updateRepository
                .findByDocumentIdAndSeqGreaterThanOrderBySeqAsc(session.getDocumentId(), from);
        for (var update : missed) {
            session.send(frame(CollabProtocol.MESSAGE_SYNC, update.getPayload()));
        }
        if (!missed.isEmpty()) {
            log.debug("Collab: bağlantıda {} paket geri oynatıldı (since={})", missed.size(), from);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession rawSession, BinaryMessage message) {
        CollabSession session = sessions.get(rawSession.getId());
        if (session == null) return;

        // .array() kullanılmıyor: BinaryMessage'ın tamponu daha büyük bir dizinin
        // dilimi olabilir ve array() o dizinin tamamını döndürür — çerçeveye
        // yabancı baytlar karışır.
        var buffer = message.getPayload();
        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        if (payload.length < 1) return;

        switch (payload[0]) {
            case CollabProtocol.MESSAGE_SYNC -> handleSync(session, payload);
            // Awareness salt-okunur katılımcılardan da kabul edilir: imlecini
            // görmek, birinin dokümanı okuduğunu bilmenin tek yolu.
            case CollabProtocol.MESSAGE_AWARENESS -> relay(session, payload);
            default -> log.trace("Collab: bilinmeyen mesaj tipi {} atıldı", payload[0]);
        }
    }

    private void handleSync(CollabSession session, byte[] payload) {
        if (!session.isCanWrite()) {
            // İstemci kısıtına güvenilmez: salt-okunur kullanıcı arayüzü
            // kilitlense bile ham WS'e elle paket gönderebilir.
            log.debug("Collab: yazma yetkisi olmayan oturumun güncellemesi atıldı: {}", session.getEmail());
            return;
        }

        // Önce röle, sonra kalıcılaştırma: gecikme kullanıcının doğrudan
        // hissettiği şey, disk yazımı değil.
        relay(session, payload);
        batcher.enqueue(session.getDocumentId(), session.getEmail(),
                Arrays.copyOfRange(payload, 1, payload.length));
    }

    private void relay(CollabSession sender, byte[] frame) {
        for (CollabSession peer : registry.peers(sender)) {
            peer.send(frame);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Collab taşıma hatası: sessionId={}, hata={}", session.getId(), exception.toString());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession rawSession, CloseStatus status) {
        CollabSession session = sessions.remove(rawSession.getId());
        if (session == null) return;
        registry.leave(session);
        log.debug("Collab bağlantısı kapandı: documentId={}, kod={}",
                session.getDocumentId(), status.getCode());
    }

    private static byte[] frame(byte type, byte[] body) {
        byte[] frame = new byte[body.length + 1];
        frame[0] = type;
        System.arraycopy(body, 0, frame, 1, body.length);
        return frame;
    }

    @SuppressWarnings("unchecked")
    private static <T> T attr(WebSocketSession session, String key) {
        return (T) session.getAttributes().get(key);
    }

    private String displayName(String email) {
        return userRepository.findByEmail(email)
                .map(u -> u.getName() != null ? u.getName() : email)
                .orElse(email);
    }
}

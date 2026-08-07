package com.scrumtools.websocket;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Bir dokümana bağlı tek istemci.
 *
 * <p>Alttaki {@link WebSocketSession} bir {@code ConcurrentWebSocketSessionDecorator}
 * ile sarılıdır: aynı oturuma birden çok röle iş parçacığından yazılır ve
 * Spring'in ham oturumu eşzamanlı gönderime karşı güvenli değildir.
 */
@Getter
@Slf4j
public class CollabSession {

    private final WebSocketSession session;
    private final UUID documentId;
    private final String email;
    private final String displayName;
    private final String color;
    private final long connectedAt;

    /**
     * Yazma yetkisi. {@code final} değil: yetki oturum sırasında düşürülebilir
     * (kullanıcı projeden çıkarılır) ve istemci kontrol mesajıyla salt-okunur moda
     * geçirilir.
     */
    private volatile boolean canWrite;

    public CollabSession(WebSocketSession session, UUID documentId, String email,
                         String displayName, String color, boolean canWrite) {
        this.session = session;
        this.documentId = documentId;
        this.email = email;
        this.displayName = displayName;
        this.color = color;
        this.canWrite = canWrite;
        this.connectedAt = System.nanoTime();
    }

    public String getId() {
        return session.getId();
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

    /**
     * Çerçeveyi gönderir; hata durumunda yalnızca log'lar.
     *
     * <p>Tek bir kopmuş istemci yüzünden röle döngüsünün patlaması, o dokümandaki
     * <i>herkesin</i> güncellemeyi kaçırması demek olurdu.
     */
    public void send(byte[] frame) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new BinaryMessage(ByteBuffer.wrap(frame)));
            }
        } catch (Exception e) {
            log.debug("Collab çerçevesi gönderilemedi: sessionId={}, {}", getId(), e.toString());
        }
    }
}

package com.scrumtools.websocket;

import com.scrumtools.security.JwtUtil;
import com.scrumtools.service.collab.CollabDocumentAccessResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * {@code /ws/collab} handshake'inde kimlik ve doküman yetkisi çözümlemesi
 * (COLLAB_WORKSPACE_PLAN.md K3).
 *
 * <p><b>Neden handshake reddedilmiyor:</b> {@code false} döndürmek HTTP 403 üretir;
 * tarayıcı WebSocket API'si bunu uygulamaya <i>1006 (anormal kapanma)</i> olarak
 * yansıtır ve ağ kesintisinden ayırt edilemez. İstemcinin "yetkim yok, yeniden
 * deneme" ile "bağlantı koptu, üstel geri çekilmeyle dene" (plan K9/R7) arasında
 * karar verebilmesi için handshake kabul edilir, karar öznitelik olarak taşınır ve
 * bağlantı {@link CollabWebSocketHandler} tarafından <b>4403</b> ile kapatılır.
 *
 * <p><b>Token neden query string'de:</b> tarayıcı WebSocket handshake'ine başlık
 * eklemeye izin vermez. Bunun bedeli JWT'nin erişim log'una düşme riskidir —
 * {@code nginx.conf}'ta bu yol için {@code access_log off} tanımlıdır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabHandshakeInterceptor implements HandshakeInterceptor {

    /** Doğrulanmış kullanıcı e-postası. Yetki verilmediyse bulunmaz. */
    public static final String ATTR_EMAIL = "collab.email";
    /** Bağlanılan dokümanın kimliği. */
    public static final String ATTR_DOCUMENT_ID = "collab.documentId";
    /** Yazma yetkisi — {@code false} ise gelen sync mesajları atılır. */
    public static final String ATTR_CAN_WRITE = "collab.canWrite";
    /** Doluysa bağlantı 4403 ile kapatılır; değeri kapanış gerekçesidir. */
    public static final String ATTR_DENIAL = "collab.denial";
    /**
     * İstemcinin REST'ten aldığı son sıra. Bağlantı kurulunca sunucu bundan
     * sonraki paketleri geri oynatır — {@code GET /state} ile bağlantı arasındaki
     * boşlukta yazılanların kaçırılmaması için (bkz. {@link CollabProtocol}).
     */
    public static final String ATTR_SINCE = "collab.since";

    private final JwtUtil jwtUtil;
    private final CollabDocumentAccessResolver accessResolver;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = queryParam(request, "token");
        String doc = queryParam(request, "doc");

        if (token == null || !jwtUtil.isTokenValid(token)) {
            attributes.put(ATTR_DENIAL, "invalid-token");
            return true;
        }
        UUID documentId = parseUuid(doc);
        if (documentId == null) {
            attributes.put(ATTR_DENIAL, "invalid-document");
            return true;
        }

        String email = jwtUtil.extractEmail(token);
        if (!accessResolver.canRead(documentId, email)) {
            attributes.put(ATTR_DENIAL, "forbidden");
            return true;
        }

        attributes.put(ATTR_EMAIL, email);
        attributes.put(ATTR_DOCUMENT_ID, documentId);
        attributes.put(ATTR_CAN_WRITE, accessResolver.canWrite(documentId, email));
        attributes.put(ATTR_SINCE, parseLong(queryParam(request, "since")));
        return true;
    }

    /** Bozuk/eksik {@code since} baştan oynatmaya düşer — fazla veri, eksik veriden iyidir. */
    private long parseLong(String value) {
        if (value == null) return 0L;
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Karar beforeHandshake'te verildi; burada yapılacak bir şey yok.
    }

    private String queryParam(ServerHttpRequest request, String name) {
        return UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(name);
    }

    private UUID parseUuid(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

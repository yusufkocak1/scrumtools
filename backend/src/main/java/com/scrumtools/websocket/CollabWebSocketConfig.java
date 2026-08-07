package com.scrumtools.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

/**
 * Ham (STOMP'suz) WebSocket uç noktası kaydı — {@code /ws/collab} (plan K3).
 *
 * <p>{@code WebSocketConfig}'teki STOMP yapılandırmasından ayrı durur: biri
 * mesajlaşma broker'ı, bu ise düz ikili taşıma. İkisi aynı uygulamada sorunsuz
 * yaşar, tek dikkat noktası aşağıdaki eşleme önceliğidir.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class CollabWebSocketConfig implements WebSocketConfigurer {

    private final CollabWebSocketHandler collabWebSocketHandler;
    private final CollabHandshakeInterceptor collabHandshakeInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://kocak.net.tr}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // SockJS uç noktası /ws altına /ws/** eşlemesi kurar; iki HandlerMapping de
        // varsayılan olarak order=1 olduğu için hangisinin önce sorulacağı garanti
        // değildir. Açıkça öne alınmazsa /ws/collab isteği SockJS'e düşebilir ve
        // "geçersiz oturum" hatası verir.
        if (registry instanceof ServletWebSocketHandlerRegistry servletRegistry) {
            servletRegistry.setOrder(0);
        }

        String[] originPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addHandler(collabWebSocketHandler, "/ws/collab")
                .addInterceptors(collabHandshakeInterceptor)
                .setAllowedOriginPatterns(originPatterns);
    }
}

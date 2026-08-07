package com.scrumtools.config;

import com.scrumtools.security.JwtUtil;
import com.scrumtools.security.WebSocketSubscriptionAuthorizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final WebSocketSubscriptionAuthorizer subscriptionAuthorizer;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://kocak.net.tr}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client'ların subscribe edeceği prefix
        registry.enableSimpleBroker("/topic");
        // Client'ların mesaj göndereceği prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] originPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(originPatterns)
                .withSockJS();
    }

    /**
     * Taşıma limitleri (COLLAB_WORKSPACE_PLAN.md Faz 0).
     *
     * <p>Spring'in varsayılanı mesaj başına 64 KB'dır. Retro panosu ve quiz durumu
     * gibi "tam durum" yayınları bugün bile bu sınıra yaklaşıyor; sınırı aşan mesaj
     * sessizce düşmez, oturumu kapatır. Ortak çalışma alanının presence/kaydedildi
     * bildirimleri de bu kanaldan geçecek.
     *
     * <p>Yoğun CRDT deltası bu kanaldan <b>geçmez</b> — ayrı ham WS uç noktası
     * kullanılır (plan K3, {@link CollabWebSocketConfig}).
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(512 * 1024)          // tek mesaj üst sınırı
                .setSendBufferSizeLimit(1024 * 1024)      // yavaş istemci için birikim tamponu
                .setSendTimeLimit(20 * 1000);             // tampon dolmuşsa oturumu kes
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                return switch (accessor.getCommand()) {
                    case CONNECT -> authenticate(accessor, message);
                    case SUBSCRIBE -> authorizeSubscription(accessor, message);
                    default -> message;
                };
            }
        });
    }

    /**
     * CONNECT'te JWT doğrulanır ve kullanıcı oturuma bağlanır.
     *
     * <p>Geçersiz/eksik token'da bağlantı <b>reddedilmez</b>, yalnızca kullanıcısız
     * kalır: stomp.js sonsuz yeniden bağlanma döngüsündedir, CONNECT'i reddetmek
     * oturumu kapanmış sekmeleri 5 sn'de bir yeniden deneyen bir fırtınaya çevirir.
     * Kullanıcısı olmayan oturum {@link #authorizeSubscription} tarafından hiçbir
     * hedefe abone edilmez, yani veri erişimi yine kapalıdır.
     */
    private Message<?> authenticate(StompHeaderAccessor accessor, Message<?> message) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return message;
        }
        String token = authHeaders.get(0);
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (jwtUtil.isTokenValid(token)) {
            String email = jwtUtil.extractEmail(token);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
            accessor.setUser(auth);
        }
        return message;
    }

    /**
     * SUBSCRIBE yetkilendirmesi (plan E3).
     *
     * <p>Yetkisiz abonelik isteği <b>düşürülür</b> (null döndürülür), hata çerçevesi
     * gönderilmez. Bunun nedeni STOMP'ta ERROR çerçevesinin tüm bağlantıyı
     * kapatması: tek hatalı abonelik, aynı sekmedeki diğer modüllerin canlı
     * bağlantısını da koparırdı. Mesaj broker'a hiç ulaşmadığı için abonelik
     * kurulmaz — sızıntı tamamen kapalıdır, yalnızca kapanışı sessizdir.
     * Teşhis için WARN log'u düşer.
     */
    private Message<?> authorizeSubscription(StompHeaderAccessor accessor, Message<?> message) {
        Principal user = accessor.getUser();
        String email = (user != null) ? user.getName() : null;
        String destination = accessor.getDestination();

        if (subscriptionAuthorizer.isAuthorized(destination, email)) {
            return message;
        }

        log.warn("Yetkisiz WebSocket aboneliği reddedildi: destination={}, user={}, sessionId={}",
                destination, email != null ? email : "<anonim>", accessor.getSessionId());
        return null;
    }
}

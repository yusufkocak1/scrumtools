package com.scrumtools.service;

import com.scrumtools.dto.AuthResponse;
import com.scrumtools.dto.ValidateTokenResponse;
import com.scrumtools.entity.PasswordSetupToken;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.TokenPurpose;
import com.scrumtools.repository.PasswordSetupTokenRepository;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.security.JwtUtil;
import com.scrumtools.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Şifre kurulum (ACCOUNT_SETUP) ve sıfırlama (PASSWORD_RESET) token akışları.
 * Ham token yalnızca e-posta linkinde yaşar; DB'de SHA-256 hash'i saklanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordTokenService {

    private static final Duration SETUP_TTL = Duration.ofDays(7);
    private static final Duration RESET_TTL = Duration.ofHours(1);
    private static final long FORGOT_COOLDOWN_MS = 60_000;

    private final PasswordSetupTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    // E-posta başına basit cooldown (user enumeration + spam önlemi)
    private final Map<String, Long> forgotRequestTimes = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Yeni token üretir, kullanıcının aynı amaçlı açık token'larını iptal eder.
     * @return e-posta linkine konacak HAM token
     */
    @Transactional
    public String createToken(User user, TokenPurpose purpose, User createdBy) {
        // Aynı kullanıcının açık token'larını geçersiz kıl (tek aktif token)
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId()).stream()
                .filter(t -> t.getPurpose() == purpose)
                .forEach(t -> t.setUsedAt(LocalDateTime.now()));

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Duration ttl = purpose == TokenPurpose.ACCOUNT_SETUP ? SETUP_TTL : RESET_TTL;
        tokenRepository.save(PasswordSetupToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plus(ttl))
                .createdBy(createdBy)
                .build());
        return rawToken;
    }

    /** Set-password sayfasının açılışında token'ın durumunu döner (hata fırlatmaz). */
    public ValidateTokenResponse validate(String rawToken) {
        return findUsableToken(rawToken)
                .map(t -> new ValidateTokenResponse(true, t.getUser().getEmail(), t.getUser().getName(), t.getPurpose()))
                .orElse(ValidateTokenResponse.invalid());
    }

    /** Şifreyi belirler, token'ı tüketir ve otomatik giriş için JWT döner. */
    @Transactional
    public AuthResponse setPassword(String rawToken, String password) {
        PasswordSetupToken token = findUsableToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bu bağlantı geçersiz veya süresi dolmuş. Lütfen yeni bir bağlantı talep edin."));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        // Kullanıcının diğer tüm açık token'larını da kapat
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                .forEach(t -> t.setUsedAt(LocalDateTime.now()));

        log.info("Şifre belirlendi ({}): {}", token.getPurpose(), user.getEmail());
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getName(), user.getEmail());
    }

    /**
     * Şifremi unuttum: kullanıcı yoksa da sessizce başarılı döner (user enumeration önlemi).
     */
    @Transactional
    public void forgotPassword(String email) {
        String normalized = email.toLowerCase().trim();

        Long last = forgotRequestTimes.get(normalized);
        long now = System.currentTimeMillis();
        if (last != null && now - last < FORGOT_COOLDOWN_MS) {
            return; // cooldown içinde — sessizce yut
        }
        forgotRequestTimes.put(normalized, now);

        userRepository.findByEmail(normalized).ifPresent(user -> {
            String rawToken = createToken(user, TokenPurpose.PASSWORD_RESET, null);
            mailService.sendPasswordReset(user, setupUrl(rawToken));
        });
    }

    /** Frontend'in set-password sayfası linki. */
    public String setupUrl(String rawToken) {
        return frontendBaseUrl + "/set-password?token=" + rawToken;
    }

    private Optional<PasswordSetupToken> findUsableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        return tokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 desteklenmiyor", e);
        }
    }
}

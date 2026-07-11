package com.scrumtools.controller;

import com.scrumtools.dto.AuthResponse;
import com.scrumtools.dto.ForgotPasswordRequest;
import com.scrumtools.dto.SetPasswordRequest;
import com.scrumtools.dto.ValidateTokenResponse;
import com.scrumtools.service.PasswordTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public şifre kurulum/sıfırlama endpoint'leri.
 * SecurityConfig'te /api/auth/password/** permitAll olarak tanımlıdır.
 */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordTokenController {

    private final PasswordTokenService passwordTokenService;

    /** Set-password sayfası açılırken token durumu (geçersizse valid=false, 200 döner). */
    @GetMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(@RequestParam String token) {
        return ResponseEntity.ok(passwordTokenService.validate(token));
    }

    /** Şifreyi belirle — başarıda JWT ile otomatik giriş. */
    @PostMapping("/set")
    public ResponseEntity<AuthResponse> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        return ResponseEntity.ok(passwordTokenService.setPassword(request.token(), request.password()));
    }

    /** Şifremi unuttum — kullanıcı bulunamasa da her zaman 200 (user enumeration önlemi). */
    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordTokenService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Bu e-posta adresi kayıtlıysa, şifre sıfırlama bağlantısı gönderildi."));
    }
}

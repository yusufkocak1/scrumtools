package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Body: { email, password, name }
     * Response: { jwt, name, email }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * POST /api/auth/login
     * Body: { email, password }
     * Response: { jwt, name, email }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * GET /api/auth/me
     * Header: Authorization: Bearer <jwt>
     * Response: { name, email }
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }

    /**
     * PUT /api/auth/change-password
     * Header: Authorization: Bearer <jwt>
     * Body: { newPassword }
     */
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/auth/update-name
     * Header: Authorization: Bearer <jwt>
     * Body: { name }
     * Response: { name, email }
     */
    @PutMapping("/update-name")
    public ResponseEntity<AuthResponse> updateName(
            Authentication authentication,
            @Valid @RequestBody UpdateNameRequest request
    ) {
        return ResponseEntity.ok(authService.updateName(authentication.getName(), request));
    }
}


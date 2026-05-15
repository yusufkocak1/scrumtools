package com.scrumtools.service;
import com.scrumtools.dto.*;
import com.scrumtools.entity.User;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    // --- UserDetailsService ---
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Kullanici bulunamadi: " + email));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
    // --- Register ---
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Bu email adresi zaten kayitli.");
        }
        User user = User.builder()
                .email(email)
                .name(request.name().trim())
                .password(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);
        String jwt = jwtUtil.generateToken(email);
        return new AuthResponse(jwt, user.getName(), user.getEmail());
    }
    // --- Login ---
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Email veya sifre hatali."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Email veya sifre hatali.");
        }
        // Son giriş zamanını güncelle
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        String jwt = jwtUtil.generateToken(email);
        return new AuthResponse(jwt, user.getName(), user.getEmail());
    }
    // --- Me ---
    public AuthResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanici bulunamadi."));
        return new AuthResponse(null, user.getName(), user.getEmail());
    }
    // --- Change Password ---
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanici bulunamadi."));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
    // --- Update Display Name ---
    @Transactional
    public AuthResponse updateName(String email, UpdateNameRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanici bulunamadi."));
        user.setName(request.name().trim());
        userRepository.save(user);
        return new AuthResponse(null, user.getName(), user.getEmail());
    }
}
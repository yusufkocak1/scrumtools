package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.SystemRole;
import com.scrumtools.entity.enums.UserStatus;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(String email) {
        User user = getUserByEmail(email);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.timezone() != null) user.setTimezone(request.timezone());
        if (request.locale() != null) user.setLocale(request.locale());

        user = userRepository.save(user);
        return toResponse(user);
    }

    // Admin endpoints
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserProfileResponse updateUserStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + userId));
        user.setStatus(status);
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateSystemRole(UUID userId, SystemRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + userId));
        user.setSystemRole(role);
        user = userRepository.save(user);
        return toResponse(user);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    private UserProfileResponse toResponse(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getAvatarUrl(),
                u.getPhone(),
                u.getTimezone(),
                u.getLocale(),
                u.getSystemRole(),
                u.getStatus(),
                u.getEmailVerified(),
                u.getLastLoginAt(),
                u.getCreatedAt()
        );
    }
}


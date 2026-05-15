package com.scrumtools.dto;

/**
 * Login/Register response.
 */
public record AuthResponse(
        String jwt,
        String name,
        String email
) {}


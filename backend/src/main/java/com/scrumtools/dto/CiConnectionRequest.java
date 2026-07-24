package com.scrumtools.dto;

import com.scrumtools.entity.enums.CiProvider;

/**
 * CI/CD bağlantısı ekleme/güncelleme isteği.
 * Güncellemede token boş bırakılırsa mevcut token korunur.
 */
public record CiConnectionRequest(
        CiProvider provider,
        String name,
        String baseUrl,
        String username,
        String token
) {}

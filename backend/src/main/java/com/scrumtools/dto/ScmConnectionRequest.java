package com.scrumtools.dto;

import com.scrumtools.entity.enums.ScmProvider;

/**
 * SCM bağlantısı ekleme/güncelleme isteği.
 * Güncellemede token boş bırakılırsa mevcut token korunur.
 */
public record ScmConnectionRequest(
        ScmProvider provider,
        String name,
        String baseUrl,
        String token,
        String username
) {}

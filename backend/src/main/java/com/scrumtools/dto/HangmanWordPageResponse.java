package com.scrumtools.dto;

import java.util.List;

/**
 * Admin panelindeki kelime listesi — dahili havuz + DB kayıtları birlikte sayfalanır.
 */
public record HangmanWordPageResponse(
        List<HangmanWordResponse> items,
        int page,
        int size,
        int totalElements,
        int totalPages
) {}

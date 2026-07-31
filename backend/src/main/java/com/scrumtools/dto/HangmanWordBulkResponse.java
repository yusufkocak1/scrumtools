package com.scrumtools.dto;

import java.util.List;

/**
 * Toplu kelime ekleme sonucu. Liste dönmez — panel sayfalı olduğu için
 * istemci ekleme sonrası bulunduğu sayfayı yeniden yükler.
 */
public record HangmanWordBulkResponse(
        int addedCount,
        int duplicateCount,
        List<String> invalidWords
) {}

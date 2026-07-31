package com.scrumtools.dto;

import java.util.List;

/**
 * Toplu kelime ekleme sonucu — iki dilin toplamı. Liste dönmez; panel sayfalı
 * olduğu için istemci ekleme sonrası bulunduğu sayfayı yeniden yükler.
 *
 * @param addedCount   eklenen kelime sayısı (tr + en)
 * @param duplicateCount havuzda zaten olduğu için atlananlar
 * @param invalidWords desene uymayanlar, "tr: kelime" biçiminde
 */
public record HangmanWordBulkResponse(
        int addedCount,
        int duplicateCount,
        List<String> invalidWords
) {}

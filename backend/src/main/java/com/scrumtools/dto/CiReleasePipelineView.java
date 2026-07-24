package com.scrumtools.dto;

import java.util.List;

/**
 * Release detayı Pipeline bölümünün tek çağrıda tüm verisi.
 *
 * @param featureEnabled  org'un paketinde CI_CD_INTEGRATION açık mı
 * @param releaseStatus   release'in mevcut durumu (buton APPROVED'da aktif)
 * @param canRun          kullanıcı çalıştırabilir mi (APPROVED + release manager/org admin + enabled eşleme)
 * @param blockedReason   canRun=false ise pasiflik nedeni (tooltip'te gösterilir), aksi halde null
 * @param mappings        seçilebilir RELEASE_PIPELINE eşlemeleri (yalnız enabled)
 * @param builds          bu release'in build tarihçesi (yeni → eski)
 */
public record CiReleasePipelineView(
        boolean featureEnabled,
        String releaseStatus,
        boolean canRun,
        String blockedReason,
        List<CiJobMappingResponse> mappings,
        List<CiBuildResponse> builds
) {}

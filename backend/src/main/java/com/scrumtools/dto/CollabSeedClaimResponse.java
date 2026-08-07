package com.scrumtools.dto;

/**
 * Tohumlama hakkı talebinin sonucu (COLLAB_WORKSPACE_PLAN.md Y1/R2).
 *
 * @param granted hak bu istemciye verildiyse {@code true}. {@code false} ise
 *                başka bir istemci sayfayı zaten aktardı; istemci <b>hiçbir şey
 *                yapmamalı</b>, normal senkron yolundan içerik gelecek.
 * @param content aktarılacak Docs HTML'i; yalnızca {@code granted} iken dolu
 */
public record CollabSeedClaimResponse(
        boolean granted,
        String content
) {
    public static CollabSeedClaimResponse denied() {
        return new CollabSeedClaimResponse(false, null);
    }
}

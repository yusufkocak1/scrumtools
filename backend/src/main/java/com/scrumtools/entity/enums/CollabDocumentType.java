package com.scrumtools.entity.enums;

/**
 * Ortak çalışma dokümanının içerik tipi (COLLAB_WORKSPACE_PLAN.md §3).
 *
 * <p>Üçü de aynı oturum/yetki/versiyon altyapısını paylaşır; ayrıştıkları yer
 * yalnızca istemcideki editör ve {@code snapshot_text}'in biçimidir.
 */
public enum CollabDocumentType {

    /** Zengin metin — TipTap. {@code snapshot_text} HTML'dir, sunucuda sanitize edilir. */
    TEXT,

    /** Kod — Monaco. {@code snapshot_text} düz metindir. */
    CODE,

    /** Hesap tablosu — Univer. Faz 3'te açılır; şimdilik oluşturulamaz. */
    SHEET
}

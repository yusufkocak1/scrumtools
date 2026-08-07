package com.scrumtools.entity.enums;

/**
 * Ortak çalışma dokümanının Docs sayfasıyla ilişkisi (COLLAB_WORKSPACE_PLAN.md §8).
 *
 * <p>Faz 1'de her doküman {@link #NONE}'dır; bağlama akışı Faz 2'de gelir.
 * Sütun şimdiden tanımlı ki Faz 2 şema değişikliği gerektirmesin.
 */
public enum CollabLinkMode {

    /** Bağımsız doküman. */
    NONE,

    /** Bir {@code DocPage} ile aynalanır — kaydedince sayfa içeriği ve versiyonu güncellenir (Y1/Y2). */
    MIRROR,

    /** Bir Docs sayfasının içine canlı gömülüdür (Y3, Faz 5). */
    EMBEDDED
}

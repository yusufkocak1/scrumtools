package com.scrumtools.dto;

import java.util.List;

/**
 * Excel içe aktarma sonucu (COLLAB_WORKSPACE_PLAN.md §10).
 *
 * <p>{@code warnings} bir hata listesi değil <b>uyum raporu</b>dur: aktarılmayan
 * özellikler (grafik, pivot, koşullu biçim, VBA) burada açıkça sayılır. Sessizce
 * atmak, kullanıcının kaybı ancak Excel'de tekrar açtığında fark etmesi demekti.
 */
public record CollabSheetImportResponse(
        CollabDocumentResponse document,
        int sheetCount,
        int cellCount,
        List<String> warnings
) {
}

package com.scrumtools.service.collab.sheet;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Hesap tablosunun okunabilir gösterimi — {@code snapshot_text} bu JSON'dır
 * (COLLAB_WORKSPACE_PLAN.md §5 / K6).
 *
 * <p>Yapı, CRDT'deki Yjs yerleşiminin birebir düz karşılığıdır: hücreler
 * {@code "R{satır}C{sütun}"} anahtarlı düz bir sözlüktür. İç içe dizi
 * kullanılmamasının nedeni §5'te yazılı — dizi indeksleri satır ekleme/silmede
 * kayar ve çakışmaları çoğaltır.
 *
 * <p><b>İndeksler 0 tabanlıdır</b> (Univer'in kendi indekslemesi). Excel'in 1
 * tabanlı gösterimine çevirme yalnızca içe/dışa aktarmada yapılır.
 *
 * <p>Sunucu bu modeli yalnızca <b>Excel'e/Excel'den</b> çevirmek için okur;
 * canlı düzenleme sırasında hiç dokunmaz (K2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollabSheetData(
        int version,
        List<Sheet> sheets,
        Map<String, Map<String, Object>> styles) {

    /** Şu an kullanılan biçim sürümü; istemci ile sunucu bunu karşılaştırır. */
    public static final int CURRENT_VERSION = 1;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Sheet(
            String id,
            String name,
            int rowCount,
            int colCount,
            Map<String, Cell> cells,
            Map<String, RowMeta> rows,
            Map<String, ColMeta> cols,
            List<Merge> merges) {
    }

    /**
     * Tek hücre.
     *
     * @param v ham değer — formül hücrelerinde <b>hesaplanmış</b> sonuç
     *          (yalnızca dışa aktarma ve okuma için; CRDT'de saklanmaz, K5)
     * @param f formül metni, {@code =} ile başlar
     * @param t Univer {@code CellValueType}: 1 metin, 2 sayı, 3 mantıksal, 4 zorunlu metin
     * @param s {@link #styles} sözlüğündeki stil anahtarı
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Cell(Object v, String f, Integer t, String s) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RowMeta(Integer h, Boolean hidden) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ColMeta(Integer w, Boolean hidden) {
    }

    /** Birleşik hücre: sol üst köşe + kapsadığı satır/sütun sayısı. */
    public record Merge(int r, int c, int rs, int cs) {
    }

    /** Kota denetimi için toplam dolu hücre sayısı. */
    public int totalCells() {
        if (sheets == null) return 0;
        return sheets.stream()
                .mapToInt(sheet -> sheet.cells() == null ? 0 : sheet.cells().size())
                .sum();
    }

    public static String cellKey(int row, int col) {
        return "R" + row + "C" + col;
    }
}

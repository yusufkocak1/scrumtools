package com.scrumtools.query;

import java.util.UUID;

/**
 * Bir zengin filtrenin akıllı filtre kuralı — çözümlenmiş hâli.
 *
 * Sıra anlamlıdır: listedeki yeri, "ilk eşleşen kazanır" kuralında önceliğini
 * belirler (bkz. RICH_FILTER_PLAN.md — K4).
 *
 * @param richFilterId sahibi olan zengin filtre — döngü tespitinde anahtarın parçası
 * @param id           öğe id'si; grafik kovalarının anahtarı
 * @param name         STQL'de değer olarak yazılan ad: {@code smart["RF"] = "Test"}
 * @param color        #RRGGBB — grafik ve görev etiketi rengi
 * @param query        kuralın çözümlenmiş sorgusu
 */
public record SmartClause(
        UUID richFilterId,
        UUID id,
        String name,
        String color,
        ParsedQuery query
) {

    /** Döngü tespiti için benzersiz anahtar. */
    public String pathKey() {
        return richFilterId + "#" + id;
    }
}

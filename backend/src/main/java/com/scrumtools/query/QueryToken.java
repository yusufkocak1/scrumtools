package com.scrumtools.query;

/**
 * Tek bir STQL token'ı.
 *
 * @param type     token türü
 * @param text     çözümlenmiş değer (STRING için tırnaklar soyulmuş hâli)
 * @param position kaynak metindeki 0-tabanlı başlangıç indeksi
 * @param length   kaynak metindeki uzunluk (tırnaklar dâhil)
 *
 * position/length çifti UI'da hatalı parçanın altını çizmek için taşınır.
 */
public record QueryToken(QueryTokenType type, String text, int position, int length) {

    public boolean is(QueryTokenType t) {
        return type == t;
    }
}

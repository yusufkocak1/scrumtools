package com.scrumtools.websocket;

/**
 * Katılımcı rengi.
 *
 * <p>Rastgele değil <b>e-postadan türetilmiş</b>: aynı kişi her oturumda,
 * her dokümanda ve her tarayıcıda aynı renkte görünür. Ekip üyeleri renkleri
 * birbirine bağlamayı öğrenir; her yenilemede renk değiştiren bir imleç bunu
 * imkânsız kılardı.
 *
 * <p>Palet, beyaz zemin üzerinde imleç etiketi olarak okunabilecek kadar koyu
 * tonlardan seçilidir.
 */
final class CollabColors {

    private CollabColors() {
    }

    private static final String[] PALETTE = {
            "#2563EB", // mavi
            "#DC2626", // kırmızı
            "#059669", // yeşil
            "#7C3AED", // mor
            "#EA580C", // turuncu
            "#0891B2", // camgöbeği
            "#DB2777", // pembe
            "#65A30D", // zeytin
            "#4F46E5", // indigo
            "#B45309", // kehribar
    };

    static String forEmail(String email) {
        if (email == null || email.isBlank()) return PALETTE[0];
        int hash = email.toLowerCase().hashCode();
        return PALETTE[Math.floorMod(hash, PALETTE.length)];
    }
}

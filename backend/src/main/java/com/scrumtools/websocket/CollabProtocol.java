package com.scrumtools.websocket;

/**
 * {@code /ws/collab} çerçeve biçimi (COLLAB_WORKSPACE_PLAN.md §6).
 *
 * <pre>
 *   [0]      mesaj tipi
 *   [1..n]   yük
 * </pre>
 *
 * <p><b>Neden y-protocols'un sync akışı kullanılmıyor:</b> o protokol, sunucunun
 * bir {@code Y.Doc} tutup durum vektörü karşılaştırması yapmasına göre tasarlanmış.
 * Bizim sunucumuz CRDT'yi açmaz (plan K2). Bunun yerine ilk durum REST'ten
 * (<code>GET /state</code>) alınır, bağlantıda sunucu {@code since}'ten sonraki
 * paketleri geri oynatır ve sonrası düz rölededir. Yjs güncellemeleri
 * <b>etkisiz-tekrarlanabilir</b> olduğu için yeniden oynatma ile rölenin
 * çakışması zararsızdır — bu, senkron el sıkışmasını tamamen gereksiz kılar.
 */
public final class CollabProtocol {

    private CollabProtocol() {
    }

    /** İstemci ↔ sunucu: yük ham bir Yjs güncellemesidir. Röle edilir ve saklanır. */
    public static final byte MESSAGE_SYNC = 0;

    /**
     * İstemci ↔ sunucu: yük ham bir awareness güncellemesidir (imleç, seçim, renk).
     * Röle edilir, <b>saklanmaz</b> — kalıcı olmayan, oturumluk veridir.
     */
    public static final byte MESSAGE_AWARENESS = 1;

    /** Sunucu → istemci: yük UTF-8 JSON. Yazar ataması, yetki düşürme, kota. */
    public static final byte MESSAGE_CONTROL = 2;

    /** Yetkisiz bağlantı — istemci yeniden denememeli. */
    public static final int CLOSE_FORBIDDEN = 4403;

    /** Eşzamanlı kullanıcı kotası doldu (plan §11) — geri çekilerek denenebilir. */
    public static final int CLOSE_QUOTA_EXCEEDED = 4429;
}

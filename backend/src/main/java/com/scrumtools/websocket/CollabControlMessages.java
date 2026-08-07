package com.scrumtools.websocket;

import java.nio.charset.StandardCharsets;

/**
 * Sunucu → istemci kontrol çerçeveleri ({@link CollabProtocol#MESSAGE_CONTROL}).
 *
 * <p>Yük küçük ve seyrek olduğu için elle JSON üretiliyor: bir {@code ObjectMapper}
 * bağımlılığı taşımak, üç alanlık iki mesaj için gereksiz.
 */
final class CollabControlMessages {

    private CollabControlMessages() {
    }

    /**
     * Bağlantı kabul edildi.
     *
     * <p>Renk <b>sunucudan</b> gelir: hem imleç dekorasyonları hem katılımcı
     * rozetleri aynı rengi kullansın diye tek kaynaktan dağıtılır. İstemci kendi
     * rengini kendisi üretseydi, awareness'taki renkle presence listesindeki renk
     * ayrışır ve aynı kişi iki farklı renkte görünürdü.
     */
    static byte[] hello(String email, String name, String color, boolean canWrite, boolean isWriter) {
        return frame("{\"type\":\"hello\""
                + ",\"email\":\"" + escape(email) + "\""
                + ",\"name\":\"" + escape(name) + "\""
                + ",\"color\":\"" + escape(color) + "\""
                + ",\"canWrite\":" + canWrite
                + ",\"isWriter\":" + isWriter + "}");
    }

    /** Yazar ataması değişti — istemci anlık görüntü sorumluluğunu alır/bırakır. */
    static byte[] writerAssigned(boolean isWriter) {
        return frame("{\"type\":\"writer\",\"isWriter\":" + isWriter + "}");
    }

    /**
     * Sunucu anlık görüntü istiyor: ham güncelleme log'u sıkıştırma eşiğini aştı.
     * İstemcinin kendi boşta-kalma zamanlayıcısı yeterli olmadığında devreye girer
     * (sürekli yazılan bir dokümanda boşta kalma hiç oluşmayabilir).
     */
    static byte[] snapshotRequested() {
        return frame("{\"type\":\"snapshotRequest\"}");
    }

    private static byte[] frame(String json) {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        byte[] frame = new byte[payload.length + 1];
        frame[0] = CollabProtocol.MESSAGE_CONTROL;
        System.arraycopy(payload, 0, frame, 1, payload.length);
        return frame;
    }

    /** Ad ve e-posta kullanıcı verisidir; tırnak veya ters bölü içerebilir. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }
}

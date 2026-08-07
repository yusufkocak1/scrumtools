package com.scrumtools.dto;

import java.util.List;

/**
 * Dokümanın açılış durumu: sıkıştırılmış CRDT anlık görüntüsü + ondan sonraki
 * ham güncellemeler (COLLAB_WORKSPACE_PLAN.md §6).
 *
 * <p><b>Neden base64:</b> sunucu Yjs güncellemelerini birleştiremez (K2 — CRDT'yi
 * açmaz), bu yüzden istemciye <i>paket listesi</i> gönderilmek zorunda. Tek bir
 * ikili gövdede taşımak özel bir çerçeveleme biçimi icat etmeyi gerektirirdi.
 * Base64'ün %33 maliyeti burada kabul edilebilir: bu yol doküman <b>başına bir
 * kez</b> işler, sıcak yol değildir — sıcak yol ham WS'tir (K3).
 */
public record CollabStateResponse(

        /** {@code Y.encodeStateAsUpdate} çıktısı; doküman hiç kaydedilmediyse null. */
        String state,

        /** {@code Y.encodeStateVector} çıktısı; null olabilir. */
        String stateVector,

        /** Anlık görüntüden sonraki güncellemeler, sırasıyla uygulanmalı. */
        List<String> updates,

        /** İstemcinin gördüğü son sıra — anlık görüntü gönderirken geri yollanır. */
        Long lastSeq,

        /** Yazma yetkisi sunucudan gelir; istemci kısıtına güvenilmez. */
        boolean canWrite
) {
}

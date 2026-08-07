package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Yazar istemcinin gönderdiği anlık görüntü (COLLAB_WORKSPACE_PLAN.md K6).
 *
 * <p>Sunucuda Yjs olmadığı için hem ikili durumu hem okunabilir metni istemci
 * üretir. Sunucu ikili kısmı <b>doğrulamaz</b> (doğrulamak için CRDT'yi açması
 * gerekirdi), ama {@code snapshotText}'i sanitize eder.
 */
public record CollabSnapshotRequest(

        @NotBlank(message = "CRDT durumu zorunlu")
        String state,

        String stateVector,

        /** TEXT → HTML, CODE → düz metin, SHEET → JSON. */
        String snapshotText,

        /**
         * İstemcinin bu anlık görüntüye dahil ettiği son sıra.
         * Sıkıştırma yalnızca bu sıraya kadar olan satırları siler; istemci
         * daha yüksek bir değer bildirirse sunucu kendi {@code lastSeq}'ine kırpar.
         */
        @NotNull(message = "seq zorunlu")
        Long seq
) {
}

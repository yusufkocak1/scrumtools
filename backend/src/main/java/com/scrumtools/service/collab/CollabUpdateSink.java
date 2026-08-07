package com.scrumtools.service.collab;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Gruplanmış CRDT güncellemelerinin kalıcılaştırma hedefi.
 *
 * <p>{@link CollabUpdateBatcher} taşıma katmanından ayrı dursun diye arayüz:
 * gruplama mantığı Faz 0'da, {@code collab_updates} tablosuna yazan uygulama
 * Faz 1'de gelir.
 */
public interface CollabUpdateSink {

    /**
     * Tek bir dokümana ait, pencerede biriken güncellemeleri yazar.
     * Çağıran hatayı yutar — burada fırlatılan istisna yayılmaz, gruba
     * ait güncellemeler kaybolur (Faz 1'de yeniden deneme politikası eklenecek).
     */
    void persist(UUID documentId, List<PendingUpdate> updates);

    /**
     * @param authorEmail güncellemeyi üreten kullanıcı; anonim röle için {@code null}
     * @param payload     ham Yjs güncellemesi (açılmaz, yorumlanmaz — plan K2)
     */
    record PendingUpdate(String authorEmail, byte[] payload, Instant receivedAt) {
    }
}

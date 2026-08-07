package com.scrumtools.service.collab;

import java.util.UUID;

/**
 * Ortak çalışma dokümanı erişim kararı — WebSocket katmanının yetki soruları
 * için tek giriş noktası (COLLAB_WORKSPACE_PLAN.md §11).
 *
 * <p>Ayrı bir arayüz olmasının nedeni, taşıma katmanının (STOMP SUBSCRIBE
 * yetkilendirmesi ve {@code /ws/collab} handshake'i) Faz 0'da yazılması ama
 * {@code CollabDocument} entity'sinin Faz 1'de gelmesidir. Faz 1 gerçek
 * uygulamayı koyup {@link DenyAllCollabDocumentAccessResolver} dosyasını siler.
 *
 * <p>Doküman bir {@code DocPage}'e bağlıysa karar {@code DocPermissionService}'e
 * devredilir — çift yetki kaynağı olmaz (plan Y1).
 */
public interface CollabDocumentAccessResolver {

    /** Dokümanı görüntüleme/dinleme yetkisi. */
    boolean canRead(UUID documentId, String email);

    /** Doküman içeriğini değiştirme yetkisi. Yetkisiz istemcinin gönderdiği
     *  sync mesajları sunucuda atılır — istemci kısıtına güvenilmez. */
    boolean canWrite(UUID documentId, String email);
}

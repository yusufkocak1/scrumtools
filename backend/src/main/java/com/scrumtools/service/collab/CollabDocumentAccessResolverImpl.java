package com.scrumtools.service.collab;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * {@link CollabDocumentAccessResolver}'ın gerçek uygulaması (Faz 1).
 *
 * <p>Faz 0'daki "her şeyi reddet" yer tutucusunun yerini alır. İnce bir kabuk
 * olmasının nedeni, taşıma katmanının (STOMP abonelik süzgeci ve {@code /ws/collab}
 * handshake'i) iş servislerine doğrudan bağlanmaması: yetki mantığı
 * {@link CollabPermissionService}'te tek yerde durur.
 */
@Component
@RequiredArgsConstructor
public class CollabDocumentAccessResolverImpl implements CollabDocumentAccessResolver {

    private final CollabDocumentService documentService;

    @Override
    public boolean canRead(UUID documentId, String email) {
        return documentService.canRead(documentId, email);
    }

    @Override
    public boolean canWrite(UUID documentId, String email) {
        return documentService.canWrite(documentId, email);
    }
}

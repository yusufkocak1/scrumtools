package com.scrumtools.service.collab;

import com.scrumtools.config.CollabProperties;
import com.scrumtools.websocket.CollabSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ham güncelleme log'unun budanması (COLLAB_WORKSPACE_PLAN.md §5, R3).
 *
 * <p>Asıl budama anlık görüntü kaydedilirken yapılır
 * ({@code CollabDocumentService.saveSnapshot}). Bu görev iki boşluğu kapatır:
 *
 * <ol>
 *   <li><b>Hiç durmayan doküman.</b> İstemci anlık görüntüyü 30 sn boşta kalınca
 *       gönderir (K6); sürekli yazılan bir dokümanda o boşluk hiç oluşmaz ve log
 *       sınırsız büyür. Burada yazardan açıkça anlık görüntü istenir.</li>
 *   <li><b>Yarım kalmış budama.</b> Anlık görüntü yazıldıktan sonra silme
 *       başarısız olduysa satırlar geride kalır; her turda tekrar denenir.</li>
 * </ol>
 *
 * <p>D3 altında bu isteğe bağlı bir iyileştirme değil <b>zorunluluk</b>: budanmayan
 * bir log Postgres diskini büyütür ve dar sunucuda diski dolduran ilk şey olur.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabCompactionScheduler {

    private final com.scrumtools.repository.CollabDocumentRepository documentRepository;
    private final com.scrumtools.repository.CollabUpdateRepository updateRepository;
    private final CollabSessionRegistry registry;
    private final CollabProperties properties;

    /**
     * Beş dakikalık tur, açılıştan 2 dk sonra başlar.
     *
     * <p>Sık çalıştırmanın anlamı yok: eşik yüzlerce güncelleme, istemci zaten
     * kendi kendine kaydediyor ve her tur bir tablo taraması demek.
     */
    @Scheduled(initialDelay = 120_000, fixedDelay = 300_000)
    @Transactional
    public void compact() {
        List<UUID> candidates =
                documentRepository.findNeedingCompaction(properties.getCompactionUpdateThreshold());
        if (candidates.isEmpty()) return;

        int requested = 0;
        for (UUID documentId : candidates) {
            // Önce geride kalmış satırları temizle — anlık görüntü zaten yazılmış
            // ama silme başarısız olmuşsa bu tek başına yeterlidir.
            documentRepository.findById(documentId).ifPresent(document -> {
                if (document.getSnapshotSeq() > 0) {
                    updateRepository.deleteUpToSeq(documentId, document.getSnapshotSeq());
                }
            });

            // Doküman hâlâ eşiğin üstündeyse tek çözüm yazardan yeni anlık
            // görüntü istemek. Doküman kapalıysa üretecek kimse yok; bir sonraki
            // açılışta kaydedilince kendiliğinden budanır.
            if (registry.requestSnapshot(documentId)) {
                requested++;
            }
        }
        log.debug("Collab sıkıştırma turu: {} aday, {} anlık görüntü istendi",
                candidates.size(), requested);
    }
}

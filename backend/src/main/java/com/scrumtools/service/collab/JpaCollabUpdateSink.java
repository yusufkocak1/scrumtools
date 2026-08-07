package com.scrumtools.service.collab;

import com.scrumtools.entity.CollabDocument;
import com.scrumtools.entity.CollabUpdate;
import com.scrumtools.entity.User;
import com.scrumtools.repository.CollabDocumentRepository;
import com.scrumtools.repository.CollabUpdateRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gruplanmış güncellemeleri {@code collab_updates} tablosuna yazar
 * (COLLAB_WORKSPACE_PLAN.md §5/§12).
 *
 * <p>Bu tablo <b>doğruluk kaynağıdır</b>: anlık görüntü yalnızca yeniden
 * oynatılacak satır sayısını azaltır. Bu yüzden burada bir yazma kaybolursa,
 * kullanıcının yazdığı metin gerçekten kaybolur — çağıran hatayı log'lar ama
 * yutar, yayın zaten yapılmıştır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaCollabUpdateSink implements CollabUpdateSink {

    private final CollabDocumentRepository documentRepository;
    private final CollabUpdateRepository updateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void persist(UUID documentId, List<PendingUpdate> updates) {
        CollabDocument document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            // Doküman gruplama penceresi içinde silinmiş olabilir; kaydedilecek
            // bir yer yok, ama sessizce yutmak da yanlış olurdu.
            log.warn("Collab güncellemesi yazılamadı, doküman yok: {}", documentId);
            return;
        }

        Long lastSeq = documentRepository.findLastSeq(documentId);
        long seq = lastSeq != null ? lastSeq : 0L;

        // Yazar çözümlemesi paket başına değil e-posta başına: yoğun bir oturumda
        // aynı kullanıcıdan onlarca paket gelir, hepsi için sorgu atmak anlamsız.
        Map<String, User> authorCache = new HashMap<>();
        List<CollabUpdate> rows = new ArrayList<>(updates.size());

        for (PendingUpdate update : updates) {
            rows.add(CollabUpdate.builder()
                    .document(document)
                    .seq(++seq)
                    .payload(update.payload())
                    .author(resolveAuthor(update.authorEmail(), authorCache))
                    .build());
        }

        updateRepository.saveAll(rows);
        documentRepository.bumpLastSeq(documentId, seq, LocalDateTime.now());
        log.trace("Collab: {} güncelleme yazıldı, documentId={}, lastSeq={}", rows.size(), documentId, seq);
    }

    private User resolveAuthor(String email, Map<String, User> cache) {
        if (email == null) return null;
        return cache.computeIfAbsent(email, e -> userRepository.findByEmail(e).orElse(null));
    }
}

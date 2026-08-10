package com.scrumtools.service.collab;

import com.scrumtools.config.CollabProperties;
import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.CollabDocumentType;
import com.scrumtools.entity.enums.CollabLinkMode;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.exception.PlanLimitExceededException;
import com.scrumtools.repository.*;
import com.scrumtools.service.DocPageService;
import com.scrumtools.service.DocPermissionService;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.PlanService;
import com.scrumtools.websocket.CollabSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ortak çalışma dokümanlarının yaşam döngüsü (COLLAB_WORKSPACE_PLAN.md §5–§6).
 *
 * <p>Sınıfın bilinçli olarak <b>yapmadığı</b> şey: CRDT'yi yorumlamak. Durum ve
 * güncellemeler opak bayt dizileridir (plan K2). İçeriği okuyabilir hâle getiren
 * tek yer istemcinin gönderdiği {@code snapshotText}'tir ve o da yalnızca
 * sanitize edilir, doğrulanmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollabDocumentService {

    private final CollabDocumentRepository documentRepository;
    private final CollabUpdateRepository updateRepository;
    private final CollabSnapshotRepository snapshotRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final DocPageRepository docPageRepository;
    private final DocPageService docPageService;
    private final DocPermissionService docPermissionService;
    private final CollabPermissionService permissionService;
    private final CollabHtmlSanitizer sanitizer;
    private final CollabUpdateBatcher batcher;
    private final CollabSessionRegistry registry;
    private final EntitlementService entitlementService;
    private final PlanService planService;
    private final CollabProperties properties;

    // ─── Oluşturma ve listeleme ──────────────────────────────────────────────

    @Transactional
    public CollabDocumentResponse create(UUID projectId, CollabDocumentRequest request) {
        User user = currentUser();
        permissionService.checkManage(projectId, user);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
        Organization organization = project.getOrganization();

        entitlementService.assertFeature(organization, PlanFeature.COLLAB_WORKSPACE);
        if (request.type() == CollabDocumentType.SHEET) {
            // Hesap tablosu ayrı bir paket özelliği (§11): ızgara istemcide çalışsa
            // da Excel G/Ç sunucudaki tek gerçek pik kalemidir.
            entitlementService.assertFeature(organization, PlanFeature.COLLAB_SHEET);
        }
        assertDocumentQuota(organization);

        Team team = resolveTeam(request.teamId(), project);

        CollabDocument document = documentRepository.save(CollabDocument.builder()
                .organization(organization)
                .project(project)
                .team(team)
                .type(request.type())
                .title(request.title().trim())
                .language(request.type() == CollabDocumentType.CODE ? request.language() : null)
                .createdBy(user)
                .updatedBy(user)
                .build());

        log.info("Collab dokümanı oluşturuldu: {} ({}, proje {})",
                document.getTitle(), document.getType(), projectId);
        return CollabDocumentResponse.from(document, true);
    }

    @Transactional(readOnly = true)
    public List<CollabDocumentSummaryResponse> list(UUID projectId, CollabDocumentType type,
                                                    UUID teamId, String query) {
        User user = currentUser();
        // Liste doküman bazlı değil proje bazlı yetkilendirilir: proje üyesi ve
        // COLLAB_READ'i olan herkes projedeki dokümanları görür.
        if (!permissionService.canReadInProject(projectId, user)) {
            throw new SecurityException("Bu projedeki dokümanları görüntüleme yetkiniz yok");
        }
        // "Filtre yok" karşılığı boş dize — null DEĞİL. Gerekçe: search() javadoc'u
        // (null parametre Hibernate'te tipsiz kalıp bytea olarak bağlanıyor).
        String normalized = query == null ? "" : query.trim();
        return documentRepository.search(projectId, type, teamId, normalized).stream()
                .map(CollabDocumentSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CollabDocumentResponse get(UUID documentId) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkRead(document, user);
        return CollabDocumentResponse.from(document, permissionService.canWrite(document, user));
    }

    // ─── Docs entegrasyonu (plan §8) ─────────────────────────────────────────

    /**
     * Y1 — bir Docs sayfasını ortak düzenlemeye açar.
     *
     * <p><b>Idempotent:</b> sayfanın zaten bir dokümanı varsa o döndürülür.
     * Aksi hâlde "Ortak Düzenle"ye ikinci kez basan kullanıcı ikinci bir doküman
     * yaratır ve sayfa iki ayrı CRDT akışıyla yazılmaya başlar.
     *
     * <p>Yetki {@code COLLAB_MANAGE} değil <b>Docs yazma</b> yetkisidir: sayfayı
     * düzenleyebilen herkes onu ortak düzenlemeye de açabilmeli, yoksa özellik
     * yalnızca proje yöneticilerinin kullanabildiği bir şeye dönüşür.
     */
    @Transactional
    public CollabDocumentResponse openForDocPage(UUID projectId, UUID pageId) {
        User user = currentUser();
        DocPage page = docPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Sayfa bulunamadı: " + pageId));
        docPermissionService.checkWriteAccess(page.getSpace(), page, user);

        var existing = documentRepository.findByDocPageId(pageId);
        if (existing.isPresent()) {
            CollabDocument document = existing.get();
            return CollabDocumentResponse.from(document, permissionService.canWrite(document, user));
        }

        Project project = page.getSpace().getProject();
        entitlementService.assertFeature(project.getOrganization(), PlanFeature.COLLAB_WORKSPACE);
        assertDocumentQuota(project.getOrganization());

        CollabDocument document = documentRepository.save(CollabDocument.builder()
                .organization(project.getOrganization())
                .project(project)
                .type(CollabDocumentType.TEXT)
                .title(page.getTitle())
                .docPage(page)
                .linkMode(CollabLinkMode.MIRROR)
                .createdBy(user)
                .updatedBy(user)
                .build());

        log.info("Docs sayfası ortak düzenlemeye açıldı: pageId={}, documentId={}", pageId, document.getId());
        return CollabDocumentResponse.from(document, true);
    }

    /** Sayfanın ortak dokümanı — yoksa boş; hiçbir şey oluşturmaz. */
    @Transactional(readOnly = true)
    public java.util.Optional<CollabDocumentResponse> findForDocPage(UUID pageId) {
        User user = currentUser();
        return documentRepository.findByDocPageId(pageId)
                .filter(d -> !d.isArchived())
                .filter(d -> permissionService.canRead(d, user))
                .map(d -> CollabDocumentResponse.from(d, permissionService.canWrite(d, user)));
    }

    /**
     * Tohumlama hakkını talep eder (Y1 adım 3, R2).
     *
     * <p>Aktarımı sunucu yapamaz — HTML'i ProseMirror üzerinden Y.Doc'a çevirmek
     * için Yjs gerekir ve sunucuda yok (K2). Bu yüzden hak <b>koşullu tek bir
     * UPDATE ile</b> tek istemciye verilir; hakkı alan istemci dönüştürüp normal
     * CRDT akışına yazar, diğerleri hiçbir şey yapmaz.
     *
     * <p>Faz 3'te aynı kilit Excel içe aktarımına da hizmet ediyor: POI'nin
     * ürettiği tablo modeli {@code snapshot_text}'te bekler, ilk açan istemci
     * onu Y.Doc'a yazar. İki farklı tohumlama yolu yerine tek mekanizma.
     */
    @Transactional
    public CollabSeedClaimResponse claimSeed(UUID documentId) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkWrite(document, user);

        if (document.getSeededAt() != null) {
            return CollabSeedClaimResponse.denied();
        }
        String content;
        if (document.getDocPage() != null) {
            content = document.getDocPage().getContent();
        } else if (document.getType() == CollabDocumentType.SHEET) {
            // İçe aktarılmış tablo: bekleyen model anlık görüntü alanında duruyor.
            content = document.getSnapshotText();
        } else {
            return CollabSeedClaimResponse.denied();
        }
        if (content == null || content.isBlank()) {
            return CollabSeedClaimResponse.denied();
        }

        int claimed = documentRepository.claimSeed(documentId, LocalDateTime.now());
        if (claimed == 0) {
            return CollabSeedClaimResponse.denied();
        }
        log.debug("Collab tohumlama hakkı verildi: documentId={}", documentId);
        return new CollabSeedClaimResponse(true, content != null ? content : "");
    }

    /**
     * Y2 — bağımsız bir dokümanı Docs'a sayfa olarak yazar ve aynalamayı başlatır.
     *
     * <p>Sayfa içeriği {@code snapshot_text}'ten gelir; yani en son anlık görüntü
     * ne ise o. Kullanıcı "Docs'a Kaydet" dediğinde son saniyenin yazdıkları
     * eksik kalabilir — bu yüzden istemci önce anlık görüntü gönderir.
     */
    @Transactional
    public CollabDocumentResponse publishToDocs(UUID documentId, CollabPublishRequest request) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkWrite(document, user);

        if (document.getDocPage() != null) {
            throw new IllegalStateException("Bu doküman zaten bir Docs sayfasına bağlı.");
        }
        if (document.getType() != CollabDocumentType.TEXT) {
            // SHEET/CODE düzleştirme Faz 3'ün konusu; sessizce bozuk HTML üretmek
            // yerine açıkça reddediliyor.
            throw new IllegalArgumentException(
                    "Şimdilik yalnızca metin dokümanları Docs'a kaydedilebilir.");
        }

        DocPageResponse created = docPageService.createPage(request.spaceId(), new DocPageRequest(
                request.title().trim(),
                sanitizer.sanitize(document.getSnapshotText()),
                request.parentPageId(),
                null,
                "Ortak çalışma alanından oluşturuldu"));

        DocPage page = docPageRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Oluşturulan sayfa okunamadı"));

        // Yeni sayfa zaten bu dokümanın içeriğiyle doğdu; tekrar tohumlanmamalı.
        documentRepository.linkDocPage(documentId, page, CollabLinkMode.MIRROR, user, LocalDateTime.now());
        markSeeded(documentId);

        log.info("Collab dokümanı Docs'a kaydedildi: documentId={}, pageId={}", documentId, page.getId());
        return CollabDocumentResponse.from(require(documentId), true);
    }

    /** Aynalamayı çözer; Docs sayfası ve doküman bağımsız yaşamaya devam eder. */
    @Transactional
    public CollabDocumentResponse unlinkDocPage(UUID documentId) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        // Bağlıyken yetki Docs'tan geliyordu; bağı çözmek Docs yazma yetkisi ister.
        permissionService.checkWrite(document, user);

        documentRepository.linkDocPage(documentId, null, CollabLinkMode.NONE, user, LocalDateTime.now());
        return CollabDocumentResponse.from(require(documentId), true);
    }

    // ─── Geçmiş (plan §6 — /history) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CollabSnapshotSummaryResponse> history(UUID documentId) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkRead(document, user);

        return snapshotRepository
                .findByDocumentIdOrderByCreatedAtDesc(documentId, Limit.of(properties.getHistoryMaxEntries()))
                .stream()
                .map(CollabSnapshotSummaryResponse::from)
                .toList();
    }

    /**
     * Bir durağın metnini döndürür.
     *
     * <p>Geri yükleme sunucuda yapılmaz: doküman o an başkaları tarafından açık
     * olabilir ve sunucunun CRDT'ye tek taraflı yazması, bağlı istemcilerin
     * durumuyla çelişirdi. İstemci bu metni alır ve <b>yeni bir düzenleme</b>
     * olarak uygular; geçmiş hep ileri akar.
     */
    @Transactional(readOnly = true)
    public String snapshotText(UUID documentId, UUID snapshotId) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkRead(document, user);

        return snapshotRepository.findByIdAndDocumentId(snapshotId, documentId)
                .map(CollabSnapshot::getSnapshotText)
                .orElseThrow(() -> new IllegalArgumentException("Geçmiş kaydı bulunamadı"));
    }

    // ─── CRDT durumu ─────────────────────────────────────────────────────────

    /**
     * Açılış durumu: anlık görüntü + ondan sonraki ham güncellemeler.
     *
     * <p>Önce gruplama tamponu boşaltılır — pencerede bekleyen güncellemeler
     * DB'ye yazılmadan okunursa, açan istemci saniyeler önce yazılmış metni
     * göremez ve yazanın bağlantısı kapandıysa o metin hiçbir yerden gelmez.
     */
    @Transactional
    public CollabStateResponse getState(UUID documentId) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkRead(document, user);
        entitlementService.assertFeature(document.getOrganization(), PlanFeature.COLLAB_WORKSPACE);

        // Entity'den okunacak her şey flush'tan ÖNCE alınıyor: gruplama tamponunu
        // boşaltmak toplu UPDATE çalıştırır ve persistence context'i temizler,
        // yani buradaki kopya detach olur.
        boolean canWrite = permissionService.canWrite(document, user);
        long snapshotSeq = document.getSnapshotSeq();
        String state = encode(document.getState());
        String stateVector = encode(document.getStateVector());

        batcher.flushDocument(documentId);

        List<String> updates = updateRepository
                .findByDocumentIdAndSeqGreaterThanOrderBySeqAsc(documentId, snapshotSeq)
                .stream()
                .map(u -> encode(u.getPayload()))
                .toList();

        Long lastSeq = documentRepository.findLastSeq(documentId);
        return new CollabStateResponse(state, stateVector, updates,
                lastSeq != null ? lastSeq : 0L, canWrite);
    }

    /**
     * Yazar istemciden gelen anlık görüntü (plan K6).
     *
     * <p>İkili durum doğrulanmaz — doğrulamak sunucuda Yjs çalıştırmak demekti ve
     * mimarinin tamamı bunun tersine kurulu. Yanlış bir durum gönderen istemci
     * yalnızca kendi dokümanını bozabilir; ham güncelleme log'u zaten
     * {@code collab_updates}'te durduğu için kurtarma mümkündür.
     */
    @Transactional
    public void saveSnapshot(UUID documentId, CollabSnapshotRequest request) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkWrite(document, user);

        Long lastSeq = documentRepository.findLastSeq(documentId);
        long currentLast = lastSeq != null ? lastSeq : 0L;
        // İstemci ileri bir sıra bildirirse kırpılır: aksi hâlde sıkıştırma,
        // anlık görüntünün içermediği güncellemeleri silerdi.
        long snapshotSeq = Math.min(request.seq(), currentLast);

        // Entity'den okunacak her şey applySnapshot'tan ÖNCE alınıyor: o toplu
        // UPDATE persistence context'i temizler ve buradaki kopya detach olur —
        // sonrasında getDocPage() gibi tembel alanlara dokunmak patlar.
        CollabDocumentType type = document.getType();
        CollabLinkMode linkMode = document.getLinkMode();
        UUID docPageId = document.getDocPage() != null ? document.getDocPage().getId() : null;

        String text = request.snapshotText();
        if (type == CollabDocumentType.TEXT) {
            text = sanitizer.sanitize(text);
        }

        LocalDateTime savedAt = LocalDateTime.now();
        documentRepository.applySnapshot(
                documentId,
                decode(request.state()),
                decode(request.stateVector()),
                text,
                snapshotSeq,
                user,
                savedAt);

        // Sıkıştırma (§5): anlık görüntünün kapsadığı ham satırlar artık gereksiz.
        // Ayrı bir zamanlanmış görev beklemek yerine burada budanıyor — tabloyu
        // küçük tutmanın en ucuz anı, tam da anlık görüntünün yazıldığı andır.
        int pruned = updateRepository.deleteUpToSeq(documentId, snapshotSeq);

        int participantCount = Math.max(1, registry.participants(documentId).size());
        recordHistory(documentId, text, snapshotSeq, participantCount, user, savedAt);
        if (docPageId != null && linkMode == CollabLinkMode.MIRROR && type == CollabDocumentType.TEXT) {
            mirrorToDocs(documentId, docPageId, text, user, participantCount);
        }

        registry.publishSaved(documentId, Map.of(
                "seq", snapshotSeq,
                "savedAt", savedAt.toString(),
                "by", user.getName() != null ? user.getName() : user.getEmail()));

        log.debug("Collab anlık görüntüsü kaydedildi: documentId={}, seq={}, budanan={}",
                documentId, snapshotSeq, pruned);
    }

    /**
     * Zaman çizelgesine durak ekler — ama her anlık görüntüde değil.
     *
     * <p>Anlık görüntü boşta kalma başına üretilir; aktif bir oturumda bu dakikada
     * birkaç kez olur. Hepsini kaydetmek geçmişi okunamaz hâle getirir ve dar
     * sunucunun diskini doldurur, o yüzden aralık ve adet sınırı var.
     */
    private void recordHistory(UUID documentId, String text, long seq,
                               int participantCount, User user, LocalDateTime savedAt) {
        if (text == null || text.isBlank()) return;

        var latest = snapshotRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId);
        if (latest.isPresent()) {
            var previous = latest.get();
            boolean tooSoon = previous.getCreatedAt()
                    .isAfter(savedAt.minusNanos(properties.getHistoryMinIntervalMs() * 1_000_000));
            // Değişmemiş içeriği tekrar kaydetmek zaman çizelgesini aynı satırla doldurur.
            if (tooSoon || text.equals(previous.getSnapshotText())) return;
        }

        snapshotRepository.save(CollabSnapshot.builder()
                // getReferenceById: yalnızca yabancı anahtar yazılacak, dokümanı
                // ikinci kez yüklemenin anlamı yok.
                .document(documentRepository.getReferenceById(documentId))
                .seq(seq)
                .snapshotText(text)
                .participantCount(participantCount)
                .createdBy(user)
                .build());
        snapshotRepository.pruneOlderThanNewest(documentId, properties.getHistoryMaxEntries());
    }

    /**
     * Y1 adım 4 — aynalanan Docs sayfasını günceller ve {@code DocPageVersion} yazar.
     *
     * <p>Hata yutuluyor: Docs tarafındaki bir sorun yüzünden anlık görüntünün
     * tamamının geri alınması, ortak çalışma dokümanını kaydedilmemiş bırakırdı.
     * Aynalama yardımcı bir çıktıdır, doğruluk kaynağı CRDT'dir.
     */
    private void mirrorToDocs(UUID documentId, UUID docPageId, String text,
                              User user, int participantCount) {
        try {
            docPageService.applyCollaborativeContent(docPageId, text, user, participantCount);
        } catch (Exception e) {
            log.error("Ortak düzenleme Docs sayfasına yazılamadı: documentId={}, pageId={}",
                    documentId, docPageId, e);
        }
    }

    private void markSeeded(UUID documentId) {
        documentRepository.markSeeded(documentId, LocalDateTime.now());
    }

    // ─── Üstveri ─────────────────────────────────────────────────────────────

    @Transactional
    public CollabDocumentResponse patch(UUID documentId, CollabDocumentPatchRequest request) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkWrite(document, user);

        String title = (request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : document.getTitle();

        String language = document.getLanguage();
        if (request.language() != null && document.getType() == CollabDocumentType.CODE) {
            language = request.language();
        }

        Team team = document.getTeam();
        if (Boolean.TRUE.equals(request.clearTeam())) {
            team = null;
        } else if (request.teamId() != null) {
            team = resolveTeam(request.teamId(), document.getProject());
        }

        documentRepository.updateMetadata(documentId, title, language, team, user, LocalDateTime.now());
        // clearAutomatically sonrası taze okuma: yukarıdaki toplu güncelleme
        // persistence context'i temizlediği için findById gerçekten DB'ye gider.
        return CollabDocumentResponse.from(require(documentId), true);
    }

    /** Yumuşak silme — ham güncelleme log'u korunur, doküman listelerden düşer. */
    @Transactional
    public void archive(UUID documentId) {
        User user = currentUser();
        CollabDocument document = require(documentId);
        permissionService.checkManage(document.getProject().getId(), user);

        documentRepository.archive(documentId, user, LocalDateTime.now());
        log.info("Collab dokümanı arşivlendi: {}", documentId);
    }

    // ─── WebSocket katmanı için (SecurityContext yok) ────────────────────────

    @Transactional(readOnly = true)
    public boolean canRead(UUID documentId, String email) {
        return withDocumentAndUser(documentId, email, permissionService::canRead);
    }

    @Transactional(readOnly = true)
    public boolean canWrite(UUID documentId, String email) {
        return withDocumentAndUser(documentId, email, permissionService::canWrite);
    }

    /** Eşzamanlı kullanıcı kotası FREE pakette geçerlidir (plan §11). */
    @Transactional(readOnly = true)
    public int concurrentUserLimit(UUID documentId) {
        return documentRepository.findById(documentId)
                .filter(d -> isDefaultPlan(d.getOrganization()))
                .map(d -> properties.getFreeMaxConcurrentUsers())
                .orElse(Integer.MAX_VALUE);
    }

    private boolean withDocumentAndUser(UUID documentId, String email,
                                        java.util.function.BiPredicate<CollabDocument, User> check) {
        if (email == null) return false;
        return documentRepository.findById(documentId)
                .filter(d -> !d.isArchived())
                .flatMap(d -> userRepository.findByEmail(email).map(u -> check.test(d, u)))
                .orElse(false);
    }

    // ─── Yardımcılar ─────────────────────────────────────────────────────────

    private CollabDocument require(UUID documentId) {
        CollabDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Doküman bulunamadı: " + documentId));
        if (document.isArchived()) {
            throw new IllegalArgumentException("Doküman arşivlenmiş: " + documentId);
        }
        return document;
    }

    private Team resolveTeam(UUID teamId, Project project) {
        if (teamId == null) return null;
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));
        // Etiket başka bir organizasyonun takımı olamaz; olsaydı liste filtresi
        // organizasyonlar arası bilgi sızdırırdı (takım adı yanıtta dönüyor).
        if (project.getOrganization() != null && team.getOrganization() != null
                && !project.getOrganization().getId().equals(team.getOrganization().getId())) {
            throw new IllegalArgumentException("Takım bu projenin organizasyonuna ait değil");
        }
        return team;
    }

    private void assertDocumentQuota(Organization organization) {
        if (organization == null || !isDefaultPlan(organization)) return;

        long count = documentRepository.countByProjectOrganizationIdAndArchivedAtIsNull(organization.getId());
        int max = properties.getFreeMaxDocuments();
        if (count >= max) {
            throw new PlanLimitExceededException("COLLAB_DOCUMENTS",
                    "Ücretsiz pakette en fazla " + max + " ortak çalışma dokümanı oluşturabilirsiniz. "
                            + "Kullanmadıklarınızı arşivleyin ya da paketinizi yükseltin.");
        }
    }

    /** Kota yalnızca varsayılan (FREE) paket için geçerlidir. */
    private boolean isDefaultPlan(Organization organization) {
        if (organization == null) return false;
        String planCode = entitlementService.getEntitlements(organization.getId()).planCode();
        return planService.getDefaultPlan().getCode().equals(planCode);
    }

    private String encode(byte[] value) {
        return value == null ? null : Base64.getEncoder().encodeToString(value);
    }

    private byte[] decode(String value) {
        return (value == null || value.isBlank()) ? null : Base64.getDecoder().decode(value);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı: " + email));
    }
}

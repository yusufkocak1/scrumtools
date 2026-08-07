package com.scrumtools.service.collab.sheet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumtools.config.CollabProperties;
import com.scrumtools.dto.CollabDocumentResponse;
import com.scrumtools.dto.CollabSheetImportResponse;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.CollabDocumentType;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.exception.PlanLimitExceededException;
import com.scrumtools.repository.CollabDocumentRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.TeamRepository;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.collab.CollabPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Hesap tablosu içe/dışa aktarma akışı (COLLAB_WORKSPACE_PLAN.md §10, Faz 3).
 *
 * <p><b>İçe aktarma neden doğrudan CRDT'ye yazmıyor:</b> sunucuda Yjs yok (K2).
 * POI'nin ürettiği model {@code snapshot_text}'e konur ve doküman
 * <i>tohumlanmamış</i> bırakılır; dokümanı ilk açan istemci Faz 2'deki aynı
 * koşullu {@code claimSeed} kilidiyle (R2) hakkı alır ve hücreleri Y.Doc'a
 * yazar. Böylece içe aktarma ile Docs tohumlaması tek bir mekanizmayı paylaşır.
 *
 * <p><b>Dışa aktarma neden anlık görüntüden okuyor:</b> formül sonuçları CRDT'de
 * saklanmıyor (K5), yalnızca istemci hesaplıyor. İstemci bu yüzden dışa
 * aktarmadan hemen önce anlık görüntü göndermek zorunda — arayüz bunu yapıyor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollabSheetService {

    private final CollabDocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final CollabPermissionService permissionService;
    private final CollabSheetImportService importService;
    private final CollabSheetExportService exportService;
    private final CollabSheetIoQueue queue;
    private final EntitlementService entitlementService;
    private final CollabProperties properties;
    private final ObjectMapper objectMapper;

    /** Dışa aktarma çıktısı — içerik tipi ve dosya adı denetleyicide kullanılır. */
    public record Export(byte[] content, String fileName, String contentType) {
    }

    // ─── İçe aktarma ─────────────────────────────────────────────────────────

    @Transactional
    public CollabSheetImportResponse importFile(UUID projectId, MultipartFile file, UUID teamId) {
        User user = currentUser();
        permissionService.checkManage(projectId, user);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
        Organization organization = project.getOrganization();
        entitlementService.assertFeature(organization, PlanFeature.COLLAB_SHEET);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş.");
        }
        // Boyut sınırı ayrıştırmadan önce: hücre kotası ancak dosya açıldıktan
        // sonra bilinir, oysa asıl bellek piki tam da o ayrıştırma anındadır.
        if (file.getSize() > properties.getSheetImportMaxBytes()) {
            throw new PlanLimitExceededException("FEATURE", "Dosya çok büyük (en fazla "
                    + (properties.getSheetImportMaxBytes() / (1024 * 1024)) + " MB).");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Dosya okunamadı.");
        }

        String originalName = file.getOriginalFilename();
        CollabSheetImportService.Result result = queue.submit(
                "import " + originalName, () -> importService.importFile(originalName, content));

        int cellCount = result.data().totalCells();
        int limit = cellLimit(organization);
        if (cellCount > limit) {
            throw new PlanLimitExceededException("FEATURE",
                    "Dosyada " + cellCount + " dolu hücre var; paketinizin sınırı " + limit + ".");
        }

        Team team = resolveTeam(teamId, project);
        CollabDocument document = documentRepository.save(CollabDocument.builder()
                .organization(organization)
                .project(project)
                .team(team)
                .type(CollabDocumentType.SHEET)
                .title(titleFrom(originalName))
                .snapshotText(writeJson(result.data()))
                .createdBy(user)
                .updatedBy(user)
                .build());

        log.info("Hesap tablosu içe aktarıldı: {} ({} hücre, proje {})",
                document.getTitle(), cellCount, projectId);

        return new CollabSheetImportResponse(
                CollabDocumentResponse.from(document, true),
                result.data().sheets() != null ? result.data().sheets().size() : 0,
                cellCount,
                result.warnings());
    }

    // ─── Dışa aktarma ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Export export(UUID documentId, String format) {
        User user = currentUser();
        CollabDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Doküman bulunamadı: " + documentId));
        permissionService.checkRead(document, user);
        entitlementService.assertFeature(document.getOrganization(), PlanFeature.COLLAB_SHEET);

        if (document.getType() != CollabDocumentType.SHEET) {
            throw new IllegalArgumentException("Yalnızca hesap tablosu dokümanları Excel'e aktarılabilir.");
        }

        String snapshot = document.getSnapshotText();
        if (snapshot == null || snapshot.isBlank()) {
            // Hiç anlık görüntü yoksa dışa aktarılacak değer de yoktur: değerler
            // istemcide hesaplanır ve yalnızca anlık görüntüyle sunucuya ulaşır.
            throw new IllegalStateException(
                    "Doküman henüz kaydedilmemiş. Tabloyu açıp birkaç saniye bekleyin.");
        }

        CollabSheetData data = readJson(snapshot);
        String safeTitle = document.getTitle().replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
        if (safeTitle.isBlank()) safeTitle = "tablo";

        String normalized = format == null ? "xlsx" : format.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "csv" -> new Export(
                    queue.submit("export csv " + documentId, () -> exportService.toCsv(data)),
                    safeTitle + ".csv",
                    "text/csv; charset=UTF-8");
            case "xlsx" -> new Export(
                    queue.submit("export xlsx " + documentId, () -> exportService.toXlsx(data)),
                    safeTitle + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> throw new IllegalArgumentException("Desteklenmeyen biçim: " + format);
        };
    }

    /** Paket bazlı hücre kotası (§10). Bilinmeyen pakette PRO sınırı uygulanır. */
    public int cellLimit(Organization organization) {
        String code = organization != null ? organization.getPlan() : null;
        return "MAX".equalsIgnoreCase(code)
                ? properties.getSheetMaxCellsMax()
                : properties.getSheetMaxCellsPro();
    }

    // ─── Yardımcılar ─────────────────────────────────────────────────────────

    private String writeJson(CollabSheetData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Tablo modeli yazılamadı: " + e.getMessage());
        }
    }

    private CollabSheetData readJson(String json) {
        try {
            return objectMapper.readValue(json, CollabSheetData.class);
        } catch (Exception e) {
            log.warn("Hesap tablosu anlık görüntüsü ayrıştırılamadı", e);
            throw new IllegalStateException("Tablo içeriği okunamadı; sayfayı yenileyip tekrar deneyin.");
        }
    }

    private static String titleFrom(String fileName) {
        if (fileName == null || fileName.isBlank()) return "İçe aktarılan tablo";
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    private Team resolveTeam(UUID teamId, Project project) {
        if (teamId == null) return null;
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));
        if (team.getOrganization() == null
                || !team.getOrganization().getId().equals(project.getOrganization().getId())) {
            throw new IllegalArgumentException("Takım bu projeyle aynı organizasyonda değil.");
        }
        return team;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı: " + email));
    }
}

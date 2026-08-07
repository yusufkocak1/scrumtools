package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.entity.enums.CollabDocumentType;
import com.scrumtools.service.collab.CollabDocumentService;
import com.scrumtools.service.collab.sheet.CollabSheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ortak çalışma alanı REST yüzeyi (COLLAB_WORKSPACE_PLAN.md §6).
 *
 * <p>Proje altında yaşar (plan D4) — Docs ile aynı hizada, tek yetki ağacı.
 *
 * <p><b>Burada olmayan şey:</b> içerik yazma. Metin bu uçlardan geçmez; CRDT
 * deltaları ayrı ham WebSocket'ten akar (K3). REST yalnızca üstveri, açılış
 * durumu ve anlık görüntü içindir.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/collab")
@RequiredArgsConstructor
public class CollabController {

    private final CollabDocumentService documentService;
    private final CollabSheetService sheetService;

    @PostMapping("/documents")
    public ResponseEntity<CollabDocumentResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CollabDocumentRequest request) {
        return ResponseEntity.ok(documentService.create(projectId, request));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<CollabDocumentSummaryResponse>> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) CollabDocumentType type,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(documentService.list(projectId, type, teamId, query));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<CollabDocumentResponse> get(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.get(documentId));
    }

    /** Açılış durumu: CRDT anlık görüntüsü + sonrasındaki ham güncellemeler. */
    @GetMapping("/documents/{documentId}/state")
    public ResponseEntity<CollabStateResponse> getState(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getState(documentId));
    }

    /** Yazar istemcinin gönderdiği anlık görüntü (K6). */
    @PostMapping("/documents/{documentId}/snapshot")
    public ResponseEntity<Void> saveSnapshot(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @Valid @RequestBody CollabSnapshotRequest request) {
        documentService.saveSnapshot(documentId, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Docs entegrasyonu (plan §8) ─────────────────────────────────────────

    /**
     * Y1 — bir Docs sayfasını ortak düzenlemeye açar. **Idempotent**: sayfanın
     * dokümanı varsa o döner, ikinci bir doküman üretilmez (R2).
     */
    @PostMapping("/documents/for-doc-page/{pageId}")
    public ResponseEntity<CollabDocumentResponse> openForDocPage(
            @PathVariable UUID projectId,
            @PathVariable UUID pageId) {
        return ResponseEntity.ok(documentService.openForDocPage(projectId, pageId));
    }

    /**
     * Sayfanın ortak dokümanı var mı — <b>oluşturmaz</b>.
     *
     * <p>Docs okuma modundaki "şu an N kişi düzenliyor" rozeti için: sayfayı
     * görüntülemek, sayfa için doküman yaratmak anlamına gelmemeli.
     */
    @GetMapping("/documents/for-doc-page/{pageId}")
    public ResponseEntity<CollabDocumentResponse> findForDocPage(
            @PathVariable UUID projectId,
            @PathVariable UUID pageId) {
        return documentService.findForDocPage(pageId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Docs içeriğini Y.Doc'a aktarma hakkını talep eder.
     *
     * <p>Hak tek bir istemciye verilir; {@code granted=false} alan istemci
     * <b>hiçbir şey yapmamalıdır</b> — aksi hâlde sayfa içeriği ikilenir (R2).
     */
    @PostMapping("/documents/{documentId}/seed-claim")
    public ResponseEntity<CollabSeedClaimResponse> claimSeed(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.claimSeed(documentId));
    }

    /** Y2 — ortak çalışma çıktısını yeni bir Docs sayfası olarak kaydeder. */
    @PostMapping("/documents/{documentId}/publish-to-docs")
    public ResponseEntity<CollabDocumentResponse> publishToDocs(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @Valid @RequestBody CollabPublishRequest request) {
        return ResponseEntity.ok(documentService.publishToDocs(documentId, request));
    }

    @DeleteMapping("/documents/{documentId}/link-doc-page")
    public ResponseEntity<CollabDocumentResponse> unlinkDocPage(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.unlinkDocPage(documentId));
    }

    // ─── Geçmiş ──────────────────────────────────────────────────────────────

    @GetMapping("/documents/{documentId}/history")
    public ResponseEntity<List<CollabSnapshotSummaryResponse>> history(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.history(documentId));
    }

    /**
     * Bir durağın metni. Geri yükleme sunucuda yapılmaz — istemci bu metni
     * <b>yeni bir düzenleme</b> olarak uygular (bkz. CollabSnapshot).
     */
    @GetMapping("/documents/{documentId}/history/{snapshotId}")
    public ResponseEntity<Map<String, String>> snapshotText(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @PathVariable UUID snapshotId) {
        return ResponseEntity.ok(Map.of(
                "snapshotText", documentService.snapshotText(documentId, snapshotId)));
    }

    @PatchMapping("/documents/{documentId}")
    public ResponseEntity<CollabDocumentResponse> patch(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @Valid @RequestBody CollabDocumentPatchRequest request) {
        return ResponseEntity.ok(documentService.patch(documentId, request));
    }

    /** Yumuşak silme — ham güncelleme log'u korunur. */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> archive(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {
        documentService.archive(documentId);
        return ResponseEntity.noContent().build();
    }

    // ─── Hesap tablosu Excel G/Ç (plan §10) ──────────────────────────────────

    /**
     * {@code .xlsx} / {@code .xlsm} / {@code .csv} yükleyip yeni bir SHEET
     * dokümanı üretir.
     *
     * <p>Doküman <b>tohumlanmamış</b> döner: içerik CRDT'ye ancak onu ilk açan
     * istemci tarafından yazılır (K2 — sunucuda Yjs yok). Cevaptaki uyum raporu
     * aktarılmayan özellikleri sayar.
     */
    @PostMapping("/documents/import")
    public ResponseEntity<CollabSheetImportResponse> importSheet(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID teamId) {
        return ResponseEntity.ok(sheetService.importFile(projectId, file, teamId));
    }

    /**
     * Tabloyu Excel ya da CSV olarak indirir.
     *
     * <p>Çıktı son anlık görüntüden üretilir; formül sonuçları yalnızca istemcide
     * hesaplandığı için (K5) arayüz bu ucu çağırmadan önce anlık görüntü
     * göndermek zorundadır.
     */
    @PostMapping("/documents/{documentId}/export")
    public ResponseEntity<byte[]> exportSheet(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "xlsx") String format) {
        CollabSheetService.Export export = sheetService.export(documentId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, export.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(export.fileName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(export.content());
    }
}

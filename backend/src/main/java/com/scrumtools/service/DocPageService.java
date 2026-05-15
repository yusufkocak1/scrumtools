package com.scrumtools.service;

import com.scrumtools.dto.DocPageRequest;
import com.scrumtools.dto.DocPageResponse;
import com.scrumtools.dto.DocPageVersionResponse;
import com.scrumtools.entity.*;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocPageService {

    private final DocPageRepository pageRepository;
    private final DocPageVersionRepository versionRepository;
    private final DocSpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final DocPermissionService permissionService;

    @Transactional
    public DocPageResponse createPage(UUID spaceId, DocPageRequest request) {
        DocSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkWriteAccess(space, null, user);

        DocPage parentPage = null;
        if (request.parentPageId() != null) {
            parentPage = pageRepository.findById(request.parentPageId())
                    .orElseThrow(() -> new RuntimeException("Üst sayfa bulunamadı"));
        }

        String slug = generateSlug(spaceId, request.title());
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : pageRepository.countBySpaceId(spaceId);

        DocPage page = DocPage.builder()
                .space(space)
                .parentPage(parentPage)
                .title(request.title())
                .slug(slug)
                .content(request.content() != null ? request.content() : "")
                .sortOrder(sortOrder)
                .createdBy(user)
                .updatedBy(user)
                .build();

        page = pageRepository.save(page);

        // İlk versiyon oluştur
        createVersion(page, user, "Sayfa oluşturuldu");

        log.info("Doc page oluşturuldu: {} (space: {})", page.getTitle(), spaceId);
        return DocPageResponse.fromFlat(page, 1);
    }

    public List<DocPageResponse> getPageTree(UUID spaceId) {
        DocSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkReadAccess(space, null, user);

        List<DocPage> rootPages = pageRepository.findBySpaceIdAndParentPageIsNullOrderBySortOrderAsc(spaceId);
        return rootPages.stream()
                .map(this::buildPageTree)
                .collect(Collectors.toList());
    }

    public DocPageResponse getPage(UUID pageId) {
        DocPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkReadAccess(page.getSpace(), page, user);

        int version = versionRepository.findMaxVersionNumber(pageId);
        return DocPageResponse.fromFlat(page, version);
    }

    @Transactional
    public DocPageResponse updatePage(UUID pageId, DocPageRequest request) {
        DocPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkWriteAccess(page.getSpace(), page, user);

        // İçerik değiştiyse versiyon kaydet
        boolean contentChanged = !page.getContent().equals(request.content());
        boolean titleChanged = !page.getTitle().equals(request.title());

        page.setTitle(request.title());
        if (request.content() != null) {
            page.setContent(request.content());
        }
        if (request.parentPageId() != null) {
            DocPage parentPage = pageRepository.findById(request.parentPageId())
                    .orElseThrow(() -> new RuntimeException("Üst sayfa bulunamadı"));
            page.setParentPage(parentPage);
        }
        if (request.sortOrder() != null) {
            page.setSortOrder(request.sortOrder());
        }
        if (titleChanged) {
            page.setSlug(generateSlug(page.getSpace().getId(), request.title()));
        }
        page.setUpdatedBy(user);
        page = pageRepository.save(page);

        if (contentChanged || titleChanged) {
            String summary = request.changeSummary() != null ? request.changeSummary() : "Sayfa güncellendi";
            createVersion(page, user, summary);
        }

        int version = versionRepository.findMaxVersionNumber(pageId);
        log.info("Doc page güncellendi: {} (v{})", page.getTitle(), version);
        return DocPageResponse.fromFlat(page, version);
    }

    @Transactional
    public void deletePage(UUID pageId) {
        DocPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkWriteAccess(page.getSpace(), page, user);

        pageRepository.delete(page);
        log.info("Doc page silindi: {}", pageId);
    }

    // ─── Versiyonlama ──────────────────────────────────────────────────────────

    public List<DocPageVersionResponse> getVersions(UUID pageId) {
        DocPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkReadAccess(page.getSpace(), page, user);

        return versionRepository.findByPageIdOrderByVersionNumberDesc(pageId).stream()
                .map(DocPageVersionResponse::from)
                .collect(Collectors.toList());
    }

    public DocPageVersionResponse getVersion(UUID pageId, int versionNumber) {
        DocPageVersion version = versionRepository.findByPageIdAndVersionNumber(pageId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Versiyon bulunamadı"));
        return DocPageVersionResponse.from(version);
    }

    @Transactional
    public DocPageResponse restoreVersion(UUID pageId, int versionNumber) {
        DocPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkWriteAccess(page.getSpace(), page, user);

        DocPageVersion version = versionRepository.findByPageIdAndVersionNumber(pageId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Versiyon bulunamadı"));

        page.setTitle(version.getTitle());
        page.setContent(version.getContent());
        page.setUpdatedBy(user);
        page = pageRepository.save(page);

        createVersion(page, user, "v" + versionNumber + " geri yüklendi");

        int latestVersion = versionRepository.findMaxVersionNumber(pageId);
        return DocPageResponse.fromFlat(page, latestVersion);
    }

    // ─── Private Helpers ────────────────────────────────────────────────────────

    private void createVersion(DocPage page, User user, String changeSummary) {
        int nextVersion = versionRepository.findMaxVersionNumber(page.getId()) + 1;

        DocPageVersion version = DocPageVersion.builder()
                .page(page)
                .versionNumber(nextVersion)
                .title(page.getTitle())
                .content(page.getContent())
                .changeSummary(changeSummary)
                .createdBy(user)
                .build();

        versionRepository.save(version);
    }

    private DocPageResponse buildPageTree(DocPage page) {
        int version = versionRepository.findMaxVersionNumber(page.getId());
        List<DocPage> children = pageRepository.findByParentPageIdOrderBySortOrderAsc(page.getId());
        List<DocPageResponse> childResponses = children.stream()
                .map(this::buildPageTree)
                .collect(Collectors.toList());
        return DocPageResponse.from(page, version, childResponses);
    }

    private String generateSlug(UUID spaceId, String title) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (base.isEmpty()) base = "page";

        String slug = base;
        int counter = 1;
        while (pageRepository.existsBySpaceIdAndSlug(spaceId, slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}


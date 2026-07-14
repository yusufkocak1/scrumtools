package com.scrumtools.service.scm;

import com.scrumtools.dto.ScmRepositoryResponse;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.ScmConnection;
import com.scrumtools.entity.ScmRepository;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.WebhookStatus;
import com.scrumtools.exception.PlanLimitExceededException;
import com.scrumtools.repository.ProjectMemberRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.ScmBranchRepository;
import com.scrumtools.repository.ScmCommitRepository;
import com.scrumtools.repository.ScmConnectionRepository;
import com.scrumtools.repository.ScmRepositoryRepository;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.PermissionService;
import com.scrumtools.service.scm.client.ScmBranchInfo;
import com.scrumtools.service.scm.client.ScmClientFactory;
import com.scrumtools.service.scm.client.ScmRepoInfo;
import com.scrumtools.service.scm.client.WebhookRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Repo–proje eşleme yönetimi. Eşleme sırasında sağlayıcıya webhook kurulur;
 * kurulamazsa eşleme yine oluşur (webhookStatus=FAILED/NONE) ve Faz 3'teki
 * poller devreye girer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScmRepoMappingService {

    /** Plan koduna göre eşlenebilir toplam repo limiti; listede olmayan plan = sınırsız. */
    private static final Map<String, Integer> REPO_LIMITS = Map.of("PRO", 3);

    private final ScmRepositoryRepository scmRepositoryRepository;
    private final ScmConnectionRepository connectionRepository;
    private final ScmBranchRepository scmBranchRepository;
    private final ScmCommitRepository scmCommitRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final PermissionService permissionService;
    private final EntitlementService entitlementService;
    private final ScmClientFactory clientFactory;

    @Value("${scm.webhook-base-url:}")
    private String webhookBaseUrl;

    /** Projeye eşlenmiş repolar — proje üyeleri görebilir. */
    @Transactional(readOnly = true)
    public List<ScmRepositoryResponse> listByProject(UUID projectId, String email) {
        checkProjectAccess(projectId, email);
        return scmRepositoryRepository.findByProjectId(projectId).stream()
                .map(ScmRepositoryResponse::from)
                .toList();
    }

    @Transactional
    public ScmRepositoryResponse map(UUID projectId, String email, UUID connectionId, String externalId) {
        permissionService.checkProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
        ScmConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("SCM bağlantısı bulunamadı."));

        // Bağlantı ile proje aynı organizasyonda olmalı
        if (project.getOrganization() == null
                || !connection.getOrganization().getId().equals(project.getOrganization().getId())) {
            throw new SecurityException("Bu bağlantı projenin organizasyonuna ait değil.");
        }

        entitlementService.assertFeature(project.getOrganization(), PlanFeature.GIT_INTEGRATION);
        checkRepoLimit(project.getOrganization().getId());

        // Bir repo aynı anda tek projeye eşlenebilir (Faz 1 kararı)
        scmRepositoryRepository.findByConnectionIdAndExternalId(connectionId, externalId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Bu repo zaten '"
                            + existing.getProject().getName() + "' projesine eşlenmiş.");
                });

        // Meta bilgiler sağlayıcıdan taze çekilir
        ScmRepoInfo info = clientFactory.forConnection(connection).getRepository(externalId);

        ScmRepository repo = ScmRepository.builder()
                .connection(connection)
                .project(project)
                .externalId(info.externalId())
                .name(info.name())
                .fullName(info.fullName())
                .defaultBranch(info.defaultBranch())
                .webUrl(info.webUrl())
                .webhookStatus(WebhookStatus.NONE)
                .lastSyncedAt(LocalDateTime.now())
                .build();
        repo = scmRepositoryRepository.save(repo);

        registerWebhook(connection, repo);
        repo = scmRepositoryRepository.save(repo);

        log.info("Repo eşlendi: {} → proje '{}' (webhook={})",
                info.fullName(), project.getName(), repo.getWebhookStatus());
        return ScmRepositoryResponse.from(repo);
    }

    @Transactional
    public void unmap(UUID projectId, String email, UUID repoId) {
        permissionService.checkProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS);
        ScmRepository repo = getRepo(projectId, repoId);

        if (repo.getWebhookExternalId() != null) {
            try {
                clientFactory.forConnection(repo.getConnection())
                        .removeWebhook(repo, repo.getWebhookExternalId());
            } catch (Exception e) {
                log.warn("Webhook silinemedi (repo={}): {}", repo.getFullName(), e.getMessage());
            }
        }
        scmBranchRepository.deleteByRepositoryId(repo.getId());
        scmCommitRepository.deleteAll(scmCommitRepository.findByRepositoryId(repo.getId()));
        scmRepositoryRepository.delete(repo);
        log.info("Repo eşlemesi kaldırıldı: {}", repo.getFullName());
    }

    /** FAILED durumundaki webhook'u yeniden kurmayı dener. */
    @Transactional
    public ScmRepositoryResponse rewebhook(UUID projectId, String email, UUID repoId) {
        permissionService.checkProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS);
        ScmRepository repo = getRepo(projectId, repoId);

        // Eski kayıt varsa best-effort temizlenir (çift webhook oluşmasın)
        if (repo.getWebhookExternalId() != null) {
            try {
                clientFactory.forConnection(repo.getConnection())
                        .removeWebhook(repo, repo.getWebhookExternalId());
            } catch (Exception ignored) {
                // eski hook zaten silinmiş olabilir
            }
            repo.setWebhookExternalId(null);
        }
        registerWebhook(repo.getConnection(), repo);
        return ScmRepositoryResponse.from(scmRepositoryRepository.save(repo));
    }

    /** Canlı branch listesi — branch açarken base seçimi için (proje üyesi yeterli). */
    @Transactional(readOnly = true)
    public List<ScmBranchInfo> listRemoteBranches(UUID projectId, String email, UUID repoId, String search) {
        checkProjectAccess(projectId, email);
        ScmRepository repo = getRepo(projectId, repoId);
        return clientFactory.forConnection(repo.getConnection()).listBranches(repo, search);
    }

    // ─── İç yardımcılar ───────────────────────────────────────────────────────

    private void registerWebhook(ScmConnection connection, ScmRepository repo) {
        if (webhookBaseUrl == null || webhookBaseUrl.isBlank()) {
            // Lokal geliştirme: public URL yok, webhook kurulamaz → poller telafi eder
            repo.setWebhookStatus(WebhookStatus.NONE);
            return;
        }
        String callbackUrl = webhookBaseUrl.replaceAll("/+$", "")
                + "/api/webhooks/scm/" + connection.getProvider().name().toLowerCase(Locale.ROOT)
                + "/" + connection.getId();
        try {
            WebhookRegistration registration = clientFactory.forConnection(connection)
                    .registerWebhook(repo, callbackUrl, connection.getWebhookSecret());
            repo.setWebhookExternalId(registration.externalId());
            repo.setWebhookStatus(WebhookStatus.ACTIVE);
        } catch (Exception e) {
            log.warn("Webhook kurulamadı (repo={}): {}", repo.getFullName(), e.getMessage());
            repo.setWebhookStatus(WebhookStatus.FAILED);
        }
    }

    private ScmRepository getRepo(UUID projectId, UUID repoId) {
        return scmRepositoryRepository.findByIdAndProjectId(repoId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Eşlenmiş repo bulunamadı."));
    }

    private void checkProjectAccess(UUID projectId, String email) {
        if (!projectMemberRepository.existsByProjectIdAndUserEmail(projectId, email)
                && !permissionService.hasProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS)) {
            throw new SecurityException("Bu projeye erişim yetkiniz yok.");
        }
    }

    private void checkRepoLimit(UUID orgId) {
        String planCode = entitlementService.getEntitlements(orgId).planCode();
        Integer limit = REPO_LIMITS.get(planCode);
        if (limit != null && scmRepositoryRepository.countByOrganizationId(orgId) >= limit) {
            throw new PlanLimitExceededException("SCM_REPOS",
                    "Paketinizin eşlenebilir repo limitine ulaştınız (" + limit + " repo). " +
                    "Daha fazla repo eşlemek için paketinizi yükseltin.");
        }
    }
}

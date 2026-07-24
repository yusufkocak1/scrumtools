package com.scrumtools.service.ci;

import com.scrumtools.dto.CiBuildResponse;
import com.scrumtools.dto.CiJobMappingResponse;
import com.scrumtools.dto.CiReleasePipelineView;
import com.scrumtools.dto.CiTaskDeployView;
import com.scrumtools.dto.CiTriggerRequest;
import com.scrumtools.entity.CiBuild;
import com.scrumtools.entity.CiConnection;
import com.scrumtools.entity.CiJobMapping;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.Release;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.enums.CiBuildContext;
import com.scrumtools.entity.enums.CiBuildStatus;
import com.scrumtools.entity.enums.CiConnectionStatus;
import com.scrumtools.entity.enums.CiJobType;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.ReleaseStatus;
import com.scrumtools.repository.CiBuildRepository;
import com.scrumtools.repository.CiJobMappingRepository;
import com.scrumtools.repository.ProjectMemberRepository;
import com.scrumtools.repository.ReleaseRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.service.AuditService;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.PermissionService;
import com.scrumtools.service.ReleaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumtools.service.ci.client.CiClient;
import com.scrumtools.service.ci.client.CiClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Build tetikleme, tarihçe sorguları ve manuel durum yenileme.
 * Durum senkronizasyonunun kendisi {@link CiBuildSyncService}'tedir; burada yalnız
 * yetki/entitlement/rate-limit kapıları, parametre çözümleme ve tetikleme yapılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CiBuildService {

    /** Rate limit: aynı task/release için son bir dakikada en çok bu kadar tetikleme (çift tık koruması). */
    private static final int RATE_LIMIT_MAX = 2;
    private static final long RATE_LIMIT_WINDOW_MINUTES = 1;
    private static final int PROJECT_HISTORY_PAGE_SIZE = 20;

    private final CiBuildRepository buildRepository;
    private final CiJobMappingRepository jobMappingRepository;
    private final TaskRepository taskRepository;
    private final ReleaseRepository releaseRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final EntitlementService entitlementService;
    private final PermissionService permissionService;
    private final ReleaseService releaseService;
    private final CiClientFactory clientFactory;
    private final CiParameterResolver parameterResolver;
    private final CiBuildSyncService syncService;
    private final AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Task deploy ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CiTaskDeployView getTaskDeployView(UUID taskId, String email) {
        Task task = getViewableTask(taskId, email);
        Project project = task.getProject();

        boolean featureEnabled = isFeatureEnabled(orgIdOf(task));
        if (project == null) {
            return new CiTaskDeployView(featureEnabled, null, false, List.of(), List.of());
        }

        List<CiJobMappingResponse> mappings = jobMappingRepository
                .findByProjectIdAndJobTypeAndEnabledTrueOrderByDisplayNameAsc(project.getId(), CiJobType.TASK_DEPLOY)
                .stream().map(CiJobMappingResponse::from).toList();

        boolean canDeploy = featureEnabled && !mappings.isEmpty();
        List<CiBuildResponse> builds = buildRepository.findByTaskIdOrderByTriggeredAtDesc(taskId)
                .stream().map(CiBuildResponse::from).toList();

        return new CiTaskDeployView(featureEnabled, project.getId(), canDeploy, mappings, builds);
    }

    @Transactional(readOnly = true)
    public List<CiBuildResponse> listTaskBuilds(UUID taskId, String email) {
        getViewableTask(taskId, email);
        return buildRepository.findByTaskIdOrderByTriggeredAtDesc(taskId)
                .stream().map(CiBuildResponse::from).toList();
    }

    @Transactional
    public CiBuildResponse triggerForTask(UUID taskId, String email, CiTriggerRequest request) {
        Task task = getViewableTask(taskId, email);
        Project project = task.getProject();
        if (project == null) throw new IllegalStateException("Görev bir projeye bağlı değil.");
        if (project.getOrganization() == null) throw new IllegalStateException("Proje bir organizasyona bağlı değil.");

        entitlementService.assertFeature(project.getOrganization(), PlanFeature.CI_CD_INTEGRATION);

        CiJobMapping mapping = requireMapping(request.mappingId(), project.getId(), CiJobType.TASK_DEPLOY);
        requireActiveConnection(mapping.getConnection());
        enforceRateLimit(buildRepository.countByTaskIdAndTriggeredAtAfter(taskId, rateWindowStart()));

        String branch = trimToNull(request.branch());
        CiParameterContext context = new CiParameterContext(
                branch, task.getCustomId(), task.getTitle(), null, project.getKey(), email);
        Map<String, String> parameters = resolveParameters(mapping, context, request.overrides());

        CiClient client = clientFactory.forConnection(mapping.getConnection());
        String queueUrl = client.triggerBuild(mapping.getJobFullName(), parameters);

        CiBuild build = CiBuild.builder()
                .jobMapping(mapping)
                .contextType(CiBuildContext.TASK)
                .taskId(task.getId())
                .taskCustomId(task.getCustomId())
                .taskTitle(task.getTitle())
                .queueItemUrl(queueUrl)
                .status(CiBuildStatus.QUEUED)
                .parametersJson(toJson(parameters))
                .branch(branch)
                .triggeredBy(email)
                .build();
        build = buildRepository.save(build);

        auditService.recordChange(task, "ciDeploy", null,
                mapping.getDisplayName() + (branch != null ? " (" + branch + ")" : ""), email);
        log.info("Task deploy tetiklendi: task={} job='{}' branch={} by={}",
                task.getCustomId(), mapping.getJobFullName(), branch, email);
        return CiBuildResponse.from(build);
    }

    // ─── Release pipeline ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CiReleasePipelineView getReleasePipelineView(UUID releaseId, String email) {
        Release release = getViewableRelease(releaseId, email);
        Project project = release.getProject();
        boolean featureEnabled = isFeatureEnabled(project.getOrganization() != null
                ? project.getOrganization().getId() : null);

        List<CiJobMappingResponse> mappings = jobMappingRepository
                .findByProjectIdAndJobTypeAndEnabledTrueOrderByDisplayNameAsc(project.getId(), CiJobType.RELEASE_PIPELINE)
                .stream().map(CiJobMappingResponse::from).toList();

        List<CiBuildResponse> builds = buildRepository.findByReleaseIdOrderByTriggeredAtDesc(releaseId)
                .stream().map(CiBuildResponse::from).toList();

        String blockedReason = releaseRunBlockedReason(release, featureEnabled, mappings.isEmpty(), email);
        boolean canRun = blockedReason == null;

        return new CiReleasePipelineView(featureEnabled, release.getStatus().name(),
                canRun, blockedReason, mappings, builds);
    }

    @Transactional(readOnly = true)
    public List<CiBuildResponse> listReleaseBuilds(UUID releaseId, String email) {
        getViewableRelease(releaseId, email);
        return buildRepository.findByReleaseIdOrderByTriggeredAtDesc(releaseId)
                .stream().map(CiBuildResponse::from).toList();
    }

    @Transactional
    public CiBuildResponse triggerForRelease(UUID releaseId, String email, CiTriggerRequest request) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release bulunamadı."));
        Project project = release.getProject();
        if (project.getOrganization() == null) throw new IllegalStateException("Proje bir organizasyona bağlı değil.");

        entitlementService.assertFeature(project.getOrganization(), PlanFeature.CI_CD_INTEGRATION);

        // Statü kapısı + yetki (release manager / org admin) — plan K5/K6
        if (release.getStatus() != ReleaseStatus.APPROVED) {
            throw new IllegalStateException("Pipeline çalıştırmak için release APPROVED durumunda olmalı.");
        }
        if (!releaseService.isManagerOrOrgAdmin(release, email)) {
            throw new SecurityException("Pipeline'ı yalnız release manager veya organizasyon yöneticisi çalıştırabilir.");
        }

        CiJobMapping mapping = requireMapping(request.mappingId(), project.getId(), CiJobType.RELEASE_PIPELINE);
        requireActiveConnection(mapping.getConnection());
        enforceRateLimit(buildRepository.countByReleaseIdAndTriggeredAtAfter(releaseId, rateWindowStart()));

        CiParameterContext context = new CiParameterContext(
                null, null, null, release.getName(), project.getKey(), email);
        Map<String, String> parameters = resolveParameters(mapping, context, request.overrides());

        CiClient client = clientFactory.forConnection(mapping.getConnection());
        String queueUrl = client.triggerBuild(mapping.getJobFullName(), parameters);

        CiBuild build = CiBuild.builder()
                .jobMapping(mapping)
                .contextType(CiBuildContext.RELEASE)
                .releaseId(release.getId())
                .releaseName(release.getName())
                .queueItemUrl(queueUrl)
                .status(CiBuildStatus.QUEUED)
                .parametersJson(toJson(parameters))
                .triggeredBy(email)
                .build();
        build = buildRepository.save(build);

        log.info("Release pipeline tetiklendi: release='{}' job='{}' by={}",
                release.getName(), mapping.getJobFullName(), email);
        return CiBuildResponse.from(build);
    }

    // ─── Proje geneli tarihçe + manuel yenileme ───────────────────────────────

    @Transactional(readOnly = true)
    public Page<CiBuildResponse> listProjectBuilds(UUID projectId, String email, String statusStr, int page) {
        checkProjectAccess(projectId, email);
        CiBuildStatus status = parseStatus(statusStr);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PROJECT_HISTORY_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "triggeredAt"));
        return buildRepository.findByProject(projectId, status, pageable).map(CiBuildResponse::from);
    }

    /** Tek build'i Jenkins'ten anında senkronize eder (kullanıcı "yenile" dediğinde). */
    @Transactional
    public CiBuildResponse refresh(UUID buildId, String email) {
        CiBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new IllegalArgumentException("Build bulunamadı."));
        checkProjectAccess(build.getJobMapping().getProject().getId(), email);

        if (build.getStatus().isTerminal()) {
            return CiBuildResponse.from(build);
        }

        CiConnection connection = build.getJobMapping().getConnection();
        CiBuildSyncService.SyncResult result;
        try {
            CiClient client = clientFactory.forConnection(connection);
            result = syncService.sync(buildId, client);
        } catch (Exception e) {
            log.warn("Manuel build yenileme başarısız (build={}): {}", buildId, e.getMessage());
            // Mevcut durumu döndür — kullanıcı tekrar deneyebilir
            return CiBuildResponse.from(buildRepository.findById(buildId).orElse(build));
        }
        syncService.applyAutoTransition(result);
        return CiBuildResponse.from(buildRepository.findById(buildId).orElse(build));
    }

    // ─── Ortak yardımcılar ────────────────────────────────────────────────────

    private Map<String, String> resolveParameters(CiJobMapping mapping, CiParameterContext context,
                                                   Map<String, String> overrides) {
        Map<String, String> resolved = new LinkedHashMap<>(
                parameterResolver.resolve(mapping.getParameterTemplate(), context));
        if (overrides != null) {
            overrides.forEach((k, v) -> {
                if (k != null && !k.isBlank()) resolved.put(k.trim(), v == null ? "" : v);
            });
        }
        return resolved;
    }

    private CiJobMapping requireMapping(UUID mappingId, UUID projectId, CiJobType expectedType) {
        if (mappingId == null) throw new IllegalArgumentException("Job eşlemesi seçilmeli.");
        CiJobMapping mapping = jobMappingRepository.findByIdAndProjectId(mappingId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Job eşlemesi bu projede bulunamadı."));
        if (!Boolean.TRUE.equals(mapping.getEnabled())) {
            throw new IllegalStateException("Bu job eşlemesi devre dışı.");
        }
        if (mapping.getJobType() != expectedType) {
            throw new IllegalArgumentException("Job eşlemesi bu işlem için uygun tipte değil ("
                    + mapping.getJobType() + ").");
        }
        return mapping;
    }

    private void requireActiveConnection(CiConnection connection) {
        if (connection.getStatus() != CiConnectionStatus.ACTIVE) {
            throw new IllegalStateException("Bu job'ın CI/CD bağlantısı aktif değil ("
                    + connection.getStatus() + "). Bağlantıyı test edip yeniden etkinleştirin.");
        }
    }

    private void enforceRateLimit(long recentCount) {
        if (recentCount >= RATE_LIMIT_MAX) {
            throw new IllegalStateException("Çok sık tetikleme. Lütfen bir dakika bekleyip tekrar deneyin.");
        }
    }

    private String releaseRunBlockedReason(Release release, boolean featureEnabled,
                                           boolean noMappings, String email) {
        if (!featureEnabled) return "Paketinizde CI/CD entegrasyonu kapalı.";
        if (noMappings) return "Bu projeye release pipeline job'ı eşlenmemiş.";
        if (release.getStatus() != ReleaseStatus.APPROVED) {
            return "Pipeline çalıştırmak için release APPROVED olmalı.";
        }
        if (!releaseService.isManagerOrOrgAdmin(release, email)) {
            return "Pipeline'ı yalnız release manager veya organizasyon yöneticisi çalıştırabilir.";
        }
        return null;
    }

    private Task getViewableTask(UUID taskId, String email) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Görev bulunamadı: " + taskId));
        UUID teamId = task.getTeam() != null ? task.getTeam().getId() : null;
        if (teamId == null || !teamMemberRepository.existsByTeamIdAndEmail(teamId, email)) {
            throw new SecurityException("Bu göreve erişim yetkiniz yok.");
        }
        return task;
    }

    private Release getViewableRelease(UUID releaseId, String email) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release bulunamadı."));
        checkProjectAccess(release.getProject().getId(), email);
        return release;
    }

    private void checkProjectAccess(UUID projectId, String email) {
        if (!projectMemberRepository.existsByProjectIdAndUserEmail(projectId, email)
                && !permissionService.hasProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS)) {
            throw new SecurityException("Bu projeye erişim yetkiniz yok.");
        }
    }

    private boolean isFeatureEnabled(UUID orgId) {
        return orgId != null
                && entitlementService.getEntitlements(orgId).features().contains(PlanFeature.CI_CD_INTEGRATION);
    }

    private UUID orgIdOf(Task task) {
        return task.getTeam() != null && task.getTeam().getOrganization() != null
                ? task.getTeam().getOrganization().getId() : null;
    }

    private CiBuildStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return CiBuildStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Geçersiz build durumu: " + statusStr);
        }
    }

    private LocalDateTime rateWindowStart() {
        return LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MINUTES);
    }

    private String toJson(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (Exception e) {
            log.warn("Parametre JSON serileştirilemedi: {}", e.getMessage());
            return null;
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

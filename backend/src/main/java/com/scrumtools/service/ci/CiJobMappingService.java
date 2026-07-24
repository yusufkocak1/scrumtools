package com.scrumtools.service.ci;

import com.scrumtools.dto.CiJobMappingRequest;
import com.scrumtools.dto.CiJobMappingResponse;
import com.scrumtools.entity.CiConnection;
import com.scrumtools.entity.CiJobMapping;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.enums.CiJobType;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.repository.CiBuildRepository;
import com.scrumtools.repository.CiConnectionRepository;
import com.scrumtools.repository.CiJobMappingRepository;
import com.scrumtools.repository.ProjectMemberRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Job–proje eşleme yönetimi. Yazma işlemleri PROJECT_MANAGE_SETTINGS
 * (SCM repo eşlemesiyle aynı), görüntüleme proje üyeliği ister.
 * Aynı job aynı projeye iki kez eşlenemez (unique connection+project+jobFullName).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CiJobMappingService {

    private final CiJobMappingRepository jobMappingRepository;
    private final CiConnectionRepository connectionRepository;
    private final CiBuildRepository buildRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final PermissionService permissionService;
    private final EntitlementService entitlementService;
    private final CiParameterResolver parameterResolver;

    /** Projeye eşlenmiş tüm job'lar — proje üyeleri görebilir. */
    @Transactional(readOnly = true)
    public List<CiJobMappingResponse> listByProject(UUID projectId, String email) {
        checkProjectAccess(projectId, email);
        return jobMappingRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(CiJobMappingResponse::from)
                .toList();
    }

    @Transactional
    public CiJobMappingResponse create(UUID projectId, String email, CiJobMappingRequest request) {
        permissionService.checkProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
        if (project.getOrganization() == null) {
            throw new IllegalStateException("Proje bir organizasyona bağlı değil.");
        }
        entitlementService.assertFeature(project.getOrganization(), PlanFeature.CI_CD_INTEGRATION);

        CiConnection connection = requireConnectionInOrg(request.connectionId(), project);
        validateCommon(request);

        String jobFullName = request.jobFullName().trim();
        if (jobMappingRepository.existsByConnectionIdAndProjectIdAndJobFullName(
                connection.getId(), projectId, jobFullName)) {
            throw new IllegalStateException("Bu job zaten bu projeye eşlenmiş: " + jobFullName);
        }

        CiJobMapping mapping = CiJobMapping.builder()
                .connection(connection)
                .project(project)
                .jobFullName(jobFullName)
                .displayName(resolveDisplayName(request, jobFullName))
                .jobType(request.jobType())
                .environment(request.environment())
                .parameterTemplate(normalizeTemplate(request.parameterTemplate()))
                .autoTransitionOnSuccess(effectiveAutoTransition(request))
                .enabled(request.enabled() == null || request.enabled())
                .createdBy(email)
                .build();

        mapping = jobMappingRepository.save(mapping);
        log.info("CI job eşlendi: {} → proje '{}' ({}/{})",
                jobFullName, project.getName(), request.jobType(), request.environment());
        return CiJobMappingResponse.from(mapping);
    }

    @Transactional
    public CiJobMappingResponse update(UUID projectId, String email, UUID mappingId,
                                       CiJobMappingRequest request) {
        permissionService.checkProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS);
        CiJobMapping mapping = getMapping(projectId, mappingId);

        // jobFullName ve bağlantı değişikliğine bilinçli olarak izin verilmez: kimlik alanları,
        // değişmesi tarihçeyi/unique kısıtı belirsizleştirir. Değiştirmek isteyen yeni eşleme kurar.
        if (!isBlank(request.displayName())) mapping.setDisplayName(request.displayName().trim());
        if (request.jobType() != null) mapping.setJobType(request.jobType());
        if (request.environment() != null) mapping.setEnvironment(request.environment());
        if (request.parameterTemplate() != null) {
            mapping.setParameterTemplate(normalizeTemplate(request.parameterTemplate()));
        }
        if (request.autoTransitionOnSuccess() != null) {
            mapping.setAutoTransitionOnSuccess(request.autoTransitionOnSuccess());
        }
        if (request.enabled() != null) mapping.setEnabled(request.enabled());

        // autoTransition yalnız RELEASE_PIPELINE'da anlamlı — tip değişmişse tutarlılığı koru
        if (mapping.getJobType() != CiJobType.RELEASE_PIPELINE) {
            mapping.setAutoTransitionOnSuccess(false);
        }

        mapping = jobMappingRepository.save(mapping);
        return CiJobMappingResponse.from(mapping);
    }

    @Transactional
    public void delete(UUID projectId, String email, UUID mappingId) {
        permissionService.checkProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS);
        CiJobMapping mapping = getMapping(projectId, mappingId);
        buildRepository.deleteByJobMappingId(mapping.getId());
        jobMappingRepository.delete(mapping);
        log.info("CI job eşlemesi kaldırıldı: {}", mapping.getJobFullName());
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private void validateCommon(CiJobMappingRequest request) {
        if (isBlank(request.jobFullName())) throw new IllegalArgumentException("Job seçilmeli.");
        if (request.jobType() == null) throw new IllegalArgumentException("Job tipi seçilmeli.");
        if (request.environment() == null) throw new IllegalArgumentException("Ortam seçilmeli.");
        parameterResolver.validateTemplate(request.parameterTemplate());
    }

    private CiConnection requireConnectionInOrg(UUID connectionId, Project project) {
        if (connectionId == null) throw new IllegalArgumentException("Bağlantı seçilmeli.");
        CiConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("CI/CD bağlantısı bulunamadı."));
        if (!connection.getOrganization().getId().equals(project.getOrganization().getId())) {
            throw new SecurityException("Bu bağlantı projenin organizasyonuna ait değil.");
        }
        return connection;
    }

    /** RELEASE_PIPELINE değilse autoTransition anlamsız — daima false'a sabitlenir. */
    private boolean effectiveAutoTransition(CiJobMappingRequest request) {
        return request.jobType() == CiJobType.RELEASE_PIPELINE
                && Boolean.TRUE.equals(request.autoTransitionOnSuccess());
    }

    private String resolveDisplayName(CiJobMappingRequest request, String jobFullName) {
        return isBlank(request.displayName()) ? jobFullName : request.displayName().trim();
    }

    private String normalizeTemplate(String template) {
        return isBlank(template) ? null : template.trim();
    }

    private CiJobMapping getMapping(UUID projectId, UUID mappingId) {
        return jobMappingRepository.findByIdAndProjectId(mappingId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Job eşlemesi bulunamadı."));
    }

    private void checkProjectAccess(UUID projectId, String email) {
        if (!projectMemberRepository.existsByProjectIdAndUserEmail(projectId, email)
                && !permissionService.hasProjectPermission(email, projectId, Permission.PROJECT_MANAGE_SETTINGS)) {
            throw new SecurityException("Bu projeye erişim yetkiniz yok.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

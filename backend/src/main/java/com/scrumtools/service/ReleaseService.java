package com.scrumtools.service;

import com.scrumtools.dto.ReleaseDeploymentResponse;
import com.scrumtools.dto.ReleaseRequest;
import com.scrumtools.dto.ReleaseResponse;
import com.scrumtools.dto.TaskResponse;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.Release;
import com.scrumtools.entity.ReleaseDeployment;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.ReleaseStatus;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Release (sürüm) yönetimi — proje seviyesinde.
 * Durum geçişleri sadece release manager veya org admin/owner tarafından yapılabilir.
 * RELEASED geçişinde bağlı tüm task'lar dağıtım tarihçesine snapshot'lanır.
 */
@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ReleaseDeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AuditService auditService;

    /** İzin verilen durum geçişleri: ileri + tek adım geri; RELEASED terminal. */
    private static final Map<ReleaseStatus, Set<ReleaseStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(Map.of(
            ReleaseStatus.OPEN, Set.of(ReleaseStatus.CODE_FREEZE, ReleaseStatus.CANCELLED),
            ReleaseStatus.CODE_FREEZE, Set.of(ReleaseStatus.REGRESSION, ReleaseStatus.OPEN, ReleaseStatus.CANCELLED),
            ReleaseStatus.REGRESSION, Set.of(ReleaseStatus.APPROVED, ReleaseStatus.CODE_FREEZE, ReleaseStatus.CANCELLED),
            ReleaseStatus.APPROVED, Set.of(ReleaseStatus.RELEASED, ReleaseStatus.REGRESSION, ReleaseStatus.CANCELLED),
            ReleaseStatus.RELEASED, Set.of(),
            ReleaseStatus.CANCELLED, Set.of(ReleaseStatus.OPEN)
    ));

    // ─── Sorgular ─────────────────────────────────────────────────────────────

    public List<ReleaseResponse> getReleasesByProject(UUID projectId) {
        return releaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Task formu dropdown'ı için release listesi. Takım birden fazla projede
     * çalışabildiğinden projectId ile hangi projenin sürümleri istendiği belirtilir;
     * verilmezse takımın birincil projesine düşülür. Takım hiçbir projeye bağlı
     * değilse boş liste döner.
     */
    @Transactional(readOnly = true)
    public List<ReleaseResponse> getReleasesByTeam(UUID teamId, UUID projectId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (projectId != null) {
            boolean belongsToTeam = team.getProjects().stream().anyMatch(p -> p.getId().equals(projectId));
            if (!belongsToTeam) {
                throw new IllegalArgumentException("Proje bu takımın çalıştığı projeler arasında değil.");
            }
            return getReleasesByProject(projectId);
        }

        if (team.getProject() != null) return getReleasesByProject(team.getProject().getId());
        return team.getProjects().stream().findFirst()
                .map(p -> getReleasesByProject(p.getId()))
                .orElseGet(List::of);
    }

    public ReleaseResponse getRelease(UUID projectId, UUID releaseId) {
        return toResponse(loadRelease(projectId, releaseId));
    }

    public List<TaskResponse> getReleaseTasks(UUID projectId, UUID releaseId) {
        loadRelease(projectId, releaseId);
        return taskRepository.findByReleaseId(releaseId)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    public List<ReleaseDeploymentResponse> getDeployments(UUID projectId, UUID releaseId) {
        loadRelease(projectId, releaseId);
        return deploymentRepository.findByReleaseIdOrderByTaskCustomIdAsc(releaseId)
                .stream()
                .map(ReleaseDeploymentResponse::from)
                .toList();
    }

    /** Bir task'ın dağıtım tarihçesi (task silinmiş/taşınmış olsa da kayıtlar korunur). */
    public List<ReleaseDeploymentResponse> getTaskDeployments(UUID taskId) {
        return deploymentRepository.findByTaskIdOrderByReleasedAtDesc(taskId)
                .stream()
                .map(ReleaseDeploymentResponse::from)
                .toList();
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public ReleaseResponse createRelease(UUID projectId, ReleaseRequest req, String userEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Sürüm adı zorunludur.");
        }
        if (releaseRepository.existsByProjectIdAndNameIgnoreCase(projectId, req.getName().trim())) {
            throw new IllegalArgumentException("Bu projede aynı isimde bir sürüm zaten var.");
        }

        String managerEmail = (req.getReleaseManagerEmail() != null && !req.getReleaseManagerEmail().isBlank())
                ? req.getReleaseManagerEmail().trim()
                : userEmail;
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Release manager kullanıcısı bulunamadı: " + managerEmail));

        Release release = Release.builder()
                .project(project)
                .name(req.getName().trim())
                .description(req.getDescription())
                .releaseManager(manager)
                .status(ReleaseStatus.OPEN)
                .freezeDate(parseDate(req.getFreezeDate()))
                .plannedReleaseDate(parseDate(req.getPlannedReleaseDate()))
                .createdBy(userEmail)
                .build();

        return toResponse(releaseRepository.save(release));
    }

    @Transactional
    public ReleaseResponse updateRelease(UUID projectId, UUID releaseId, ReleaseRequest req, String userEmail) {
        Release release = loadRelease(projectId, releaseId);
        checkManagerOrOrgAdmin(release, userEmail);

        if (release.getStatus() == ReleaseStatus.RELEASED || release.getStatus() == ReleaseStatus.CANCELLED) {
            throw new IllegalStateException("Yayınlanmış veya iptal edilmiş sürüm düzenlenemez.");
        }

        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(release.getName())
                    && releaseRepository.existsByProjectIdAndNameIgnoreCase(projectId, newName)) {
                throw new IllegalArgumentException("Bu projede aynı isimde bir sürüm zaten var.");
            }
            release.setName(newName);
        }
        if (req.getDescription() != null) release.setDescription(req.getDescription());
        if (req.getReleaseManagerEmail() != null && !req.getReleaseManagerEmail().isBlank()) {
            User manager = userRepository.findByEmail(req.getReleaseManagerEmail().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Release manager kullanıcısı bulunamadı: " + req.getReleaseManagerEmail()));
            release.setReleaseManager(manager);
        }
        if (req.getFreezeDate() != null) release.setFreezeDate(parseDate(req.getFreezeDate()));
        if (req.getPlannedReleaseDate() != null) release.setPlannedReleaseDate(parseDate(req.getPlannedReleaseDate()));

        return toResponse(releaseRepository.save(release));
    }

    @Transactional
    public void deleteRelease(UUID projectId, UUID releaseId, String userEmail) {
        Release release = loadRelease(projectId, releaseId);
        checkManagerOrOrgAdmin(release, userEmail);

        if (deploymentRepository.existsByReleaseId(releaseId)) {
            throw new IllegalStateException("Dağıtım tarihçesi olan (yayınlanmış) sürüm silinemez.");
        }

        // Bağlı task'ların release alanını temizle + tarihçele
        for (Task task : taskRepository.findByReleaseId(releaseId)) {
            auditService.recordChange(task, "release", release.getName(), null, userEmail);
            task.setRelease(null);
            taskRepository.save(task);
        }

        releaseRepository.delete(release);
    }

    // ─── Durum Geçişi ─────────────────────────────────────────────────────────

    @Transactional
    public ReleaseResponse updateStatus(UUID projectId, UUID releaseId, String newStatusStr, String userEmail) {
        Release release = loadRelease(projectId, releaseId);
        checkManagerOrOrgAdmin(release, userEmail);

        ReleaseStatus newStatus;
        try {
            newStatus = ReleaseStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Geçersiz sürüm durumu: " + newStatusStr);
        }

        ReleaseStatus current = release.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Geçersiz durum geçişi: " + current + " → " + newStatus);
        }

        release.setStatus(newStatus);

        if (newStatus == ReleaseStatus.RELEASED) {
            release.setActualReleaseDate(LocalDateTime.now());
            snapshotDeployments(release, userEmail);
        }

        return toResponse(releaseRepository.save(release));
    }

    /**
     * Dağıtım tarihçesi: release'e bağlı tüm task'ları değişmez kayıt olarak snapshot'la
     * ve her task'ın kendi tarihçesine "deployedInRelease" kaydı düş.
     */
    private void snapshotDeployments(Release release, String userEmail) {
        for (Task task : taskRepository.findByReleaseId(release.getId())) {
            ReleaseDeployment deployment = ReleaseDeployment.builder()
                    .release(release)
                    .taskId(task.getId())
                    .taskCustomId(task.getCustomId())
                    .taskTitle(task.getTitle())
                    .taskStatusAtRelease(task.getStatus())
                    .releasedBy(userEmail)
                    .build();
            deploymentRepository.save(deployment);
            auditService.recordChange(task, "deployedInRelease", null, release.getName(), userEmail);
        }
    }

    // ─── Task-Release Bağlama Kuralları (TaskService kullanır) ───────────────

    /**
     * Task'ın bir release'e bağlanması öncesi doğrulama:
     * - Release, task'ın takımının bağlı olduğu projeye ait olmalı.
     * - OPEN: herkes bağlayabilir. CODE_FREEZE/REGRESSION/APPROVED: sadece manager veya org admin
     *   (paket kapandıktan sonra hotfix ancak manager onayıyla girer).
     * - RELEASED/CANCELLED: kimse bağlayamaz.
     */
    public void validateTaskLink(Release release, Project taskProject, String userEmail) {
        // Sürüm proje seviyesinde; görev artık kendi projesini taşıdığı için kıyas
        // takımın değil görevin projesi üzerinden yapılır (takım çok projede çalışabilir).
        if (taskProject == null || !taskProject.getId().equals(release.getProject().getId())) {
            throw new IllegalArgumentException("Sürüm, görevin ait olduğu projeye ait değil.");
        }

        ReleaseStatus status = release.getStatus();
        if (status == ReleaseStatus.RELEASED || status == ReleaseStatus.CANCELLED) {
            throw new IllegalStateException("Yayınlanmış veya iptal edilmiş sürüme görev eklenemez.");
        }
        if (status != ReleaseStatus.OPEN && !isManagerOrOrgAdmin(release, userEmail)) {
            throw new SecurityException("Paket kapandıktan sonra sürüme görev ekleme sadece release manager veya organizasyon yöneticisi tarafından yapılabilir.");
        }
    }

    /**
     * Task'ın mevcut release'inden çıkarılması öncesi doğrulama:
     * - Freeze sonrası (CODE_FREEZE/REGRESSION/APPROVED) paketten görev çıkarmak manager/org admin ister.
     * - RELEASED/CANCELLED release'ten çıkarmak serbesttir — dağıtım tarihçesi snapshot'ta
     *   korunduğu için tamamlanamayan iş sonraki sürüme taşınabilir.
     */
    public void validateTaskUnlink(Release release, String userEmail) {
        ReleaseStatus status = release.getStatus();
        boolean frozen = status == ReleaseStatus.CODE_FREEZE
                || status == ReleaseStatus.REGRESSION
                || status == ReleaseStatus.APPROVED;
        if (frozen && !isManagerOrOrgAdmin(release, userEmail)) {
            throw new SecurityException("Paket kapandıktan sonra sürümden görev çıkarma sadece release manager veya organizasyon yöneticisi tarafından yapılabilir.");
        }
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private Release loadRelease(UUID projectId, UUID releaseId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new RuntimeException("Release not found"));
        if (!release.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Release does not belong to project");
        }
        return release;
    }

    private void checkManagerOrOrgAdmin(Release release, String userEmail) {
        if (!isManagerOrOrgAdmin(release, userEmail)) {
            throw new SecurityException("Bu işlemi sadece release manager veya organizasyon yöneticisi yapabilir.");
        }
    }

    /** Release manager veya org admin/owner mi — CI pipeline tetikleme yetkisi de bunu kullanır. */
    public boolean isManagerOrOrgAdmin(Release release, String userEmail) {
        if (release.getReleaseManager() != null
                && release.getReleaseManager().getEmail().equalsIgnoreCase(userEmail)) {
            return true;
        }
        if (release.getProject().getOrganization() == null) return false;
        UUID orgId = release.getProject().getOrganization().getId();
        return organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(
                orgId, userEmail, List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN));
    }

    private ReleaseResponse toResponse(Release release) {
        long taskCount = taskRepository.countByReleaseId(release.getId());
        long doneTaskCount = taskRepository.countByReleaseIdAndStatus(release.getId(), "Done");
        return ReleaseResponse.from(release, taskCount, doneTaskCount);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return LocalDate.parse(dateStr);
    }
}

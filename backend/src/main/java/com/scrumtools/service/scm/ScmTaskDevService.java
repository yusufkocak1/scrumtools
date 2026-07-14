package com.scrumtools.service.scm;

import com.scrumtools.dto.ScmBranchResponse;
import com.scrumtools.dto.ScmCommitResponse;
import com.scrumtools.dto.ScmRepositoryResponse;
import com.scrumtools.dto.TaskScmResponse;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.ScmRepository;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.ScmProvider;
import com.scrumtools.repository.ScmBranchRepository;
import com.scrumtools.repository.ScmCommitRepository;
import com.scrumtools.repository.ScmRepositoryRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.repository.UserScmAccountRepository;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Task detayındaki Geliştirme (Dev) paneli verisini toplar.
 * Görüntüleme takım üyeliğiyle serbesttir (downgrade sonrası veri kaybolmaz);
 * yazma işlemleri (branch açma) featureEnabled + SCM_CREATE_BRANCH ister.
 */
@Service
@RequiredArgsConstructor
public class ScmTaskDevService {

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ScmRepositoryRepository scmRepositoryRepository;
    private final ScmBranchRepository scmBranchRepository;
    private final ScmCommitRepository scmCommitRepository;
    private final UserScmAccountRepository userScmAccountRepository;
    private final PermissionService permissionService;
    private final EntitlementService entitlementService;

    @Transactional(readOnly = true)
    public TaskScmResponse getTaskScm(UUID teamId, UUID taskId, String email) {
        if (!teamMemberRepository.existsByTeamIdAndEmail(teamId, email)) {
            throw new SecurityException("Bu takıma erişim yetkiniz yok.");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task bulunamadı: " + taskId));
        if (!task.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("Task bu takıma ait değil.");
        }

        boolean featureEnabled = task.getTeam().getOrganization() != null
                && entitlementService.getEntitlements(task.getTeam().getOrganization().getId())
                        .features().contains(PlanFeature.GIT_INTEGRATION);

        Project project = task.getTeam().getProject();
        if (project == null) {
            // Takım projeye bağlı değil — DevPanel boş durum gösterir (§13)
            return new TaskScmResponse(featureEnabled, false, null,
                    false, false, false, List.of(), List.of(), List.of());
        }

        List<ScmRepository> repos = scmRepositoryRepository.findByProjectId(project.getId());

        boolean canCreateBranch = featureEnabled && !repos.isEmpty()
                && permissionService.hasProjectPermission(email, project.getId(), Permission.SCM_CREATE_BRANCH);
        boolean canManageRepos =
                permissionService.hasProjectPermission(email, project.getId(), Permission.PROJECT_MANAGE_SETTINGS);

        // Kullanıcının, eşlenmiş repoların sağlayıcılarından biriyle bağlı hesabı var mı
        Set<ScmProvider> repoProviders = repos.stream()
                .map(r -> r.getConnection().getProvider())
                .collect(Collectors.toSet());
        boolean hasUserAccount = userScmAccountRepository.findByUserEmail(email).stream()
                .anyMatch(a -> repoProviders.contains(a.getProvider()));

        return new TaskScmResponse(
                featureEnabled,
                true,
                project.getId(),
                canCreateBranch,
                canManageRepos,
                hasUserAccount,
                repos.stream().map(ScmRepositoryResponse::from).toList(),
                scmBranchRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                        .map(ScmBranchResponse::from).toList(),
                scmCommitRepository.findByTaskId(taskId).stream()
                        .map(ScmCommitResponse::from).toList()
        );
    }
}

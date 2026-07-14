package com.scrumtools.service.scm;

import com.scrumtools.dto.ScmBranchCreateRequest;
import com.scrumtools.dto.ScmBranchResponse;
import com.scrumtools.dto.ScmCommitResponse;
import com.scrumtools.dto.ScmRepositoryResponse;
import com.scrumtools.dto.TaskScmResponse;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.ScmBranch;
import com.scrumtools.entity.ScmConnection;
import com.scrumtools.entity.ScmRepository;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.UserScmAccount;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.ScmBranchStatus;
import com.scrumtools.entity.enums.ScmConnectionStatus;
import com.scrumtools.entity.enums.ScmProvider;
import com.scrumtools.repository.ScmBranchRepository;
import com.scrumtools.repository.ScmCommitRepository;
import com.scrumtools.repository.ScmRepositoryRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.repository.UserScmAccountRepository;
import com.scrumtools.service.AuditService;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.PermissionService;
import com.scrumtools.service.scm.client.ScmApiException;
import com.scrumtools.service.scm.client.ScmBranchInfo;
import com.scrumtools.service.scm.client.ScmClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
@Slf4j
public class ScmTaskDevService {

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ScmRepositoryRepository scmRepositoryRepository;
    private final ScmBranchRepository scmBranchRepository;
    private final ScmCommitRepository scmCommitRepository;
    private final UserScmAccountRepository userScmAccountRepository;
    private final PermissionService permissionService;
    private final EntitlementService entitlementService;
    private final ScmClientFactory clientFactory;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public TaskScmResponse getTaskScm(UUID teamId, UUID taskId, String email) {
        Task task = getTaskInTeam(teamId, taskId, email);

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

    /**
     * Task'tan sağlayıcıda branch açar ve task'a bağlar. Kullanıcının kendi
     * SCM hesabı (aynı provider+baseUrl) varsa onun tokenıyla açılır — branch
     * sağlayıcıda kullanıcının adına görünür; yoksa org bağlantısının tokenına düşülür.
     */
    @Transactional
    public ScmBranchResponse createBranch(UUID teamId, UUID taskId, String email,
                                          ScmBranchCreateRequest request) {
        Task task = getTaskInTeam(teamId, taskId, email);
        Project project = task.getTeam().getProject();
        if (project == null) {
            throw new IllegalStateException("Takım bir projeye bağlı değil.");
        }
        if (task.getTeam().getOrganization() == null) {
            throw new IllegalStateException("Takım bir organizasyona bağlı değil.");
        }
        entitlementService.assertFeature(task.getTeam().getOrganization(), PlanFeature.GIT_INTEGRATION);
        permissionService.checkProjectPermission(email, project.getId(), Permission.SCM_CREATE_BRANCH);

        if (request.repositoryId() == null) {
            throw new IllegalArgumentException("Repo seçilmeli.");
        }
        ScmRepository repo = scmRepositoryRepository
                .findByIdAndProjectId(request.repositoryId(), project.getId())
                .orElseThrow(() -> new IllegalArgumentException("Eşlenmiş repo bulunamadı."));

        String branchName = ScmBranchNames.validate(request.branchName());
        String sourceRef = isBlank(request.sourceRef())
                ? repo.getDefaultBranch() : request.sourceRef().trim();
        if (isBlank(sourceRef)) {
            throw new IllegalArgumentException("Kaynak branch belirtilmeli.");
        }

        scmBranchRepository.findByRepositoryIdAndTaskIdAndName(repo.getId(), taskId, branchName)
                .ifPresent(b -> {
                    throw new IllegalStateException("Bu branch zaten bu task'a bağlı: " + branchName);
                });

        ScmBranchInfo info = createBranchOnProvider(repo, email, branchName, sourceRef);

        ScmBranch branch = ScmBranch.builder()
                .repository(repo)
                .task(task)
                .name(info.name() != null ? info.name() : branchName)
                .webUrl(info.webUrl())
                .createdViaApp(true)
                .createdBy(email)
                .status(ScmBranchStatus.ACTIVE)
                .lastCommitSha(info.sha())
                .build();
        branch = scmBranchRepository.save(branch);

        auditService.recordChange(task, "branch", null, branchName, email);
        log.info("Branch açıldı: {} → {} (task={})", repo.getFullName(), branchName, task.getCustomId());
        return ScmBranchResponse.from(branch);
    }

    /**
     * Branch'i sağlayıcıda oluşturur. Kullanıcı tokenı geçersiz çıkarsa hesap
     * TOKEN_INVALID işaretlenir ve org bağlantısının tokenıyla bir kez daha denenir.
     */
    private ScmBranchInfo createBranchOnProvider(ScmRepository repo, String email,
                                                 String branchName, String sourceRef) {
        ScmConnection connection = repo.getConnection();
        UserScmAccount userAccount = userScmAccountRepository
                .findByUserEmailAndProvider(email, connection.getProvider()).stream()
                .filter(a -> a.getStatus() == ScmConnectionStatus.ACTIVE)
                .filter(a -> Objects.equals(a.getBaseUrl(), connection.getBaseUrl()))
                .findFirst()
                .orElse(null);

        if (userAccount == null) {
            return clientFactory.forConnection(connection).createBranch(repo, branchName, sourceRef);
        }
        try {
            return clientFactory.forUserAccount(userAccount).createBranch(repo, branchName, sourceRef);
        } catch (ScmApiException e) {
            if (!e.isAuthFailure()) throw e;
            userAccount.setStatus(ScmConnectionStatus.TOKEN_INVALID);
            userScmAccountRepository.save(userAccount);
            log.warn("Kişisel SCM tokenı geçersiz ({}), org bağlantısına düşülüyor.", email);
            return clientFactory.forConnection(connection).createBranch(repo, branchName, sourceRef);
        }
    }

    private Task getTaskInTeam(UUID teamId, UUID taskId, String email) {
        if (!teamMemberRepository.existsByTeamIdAndEmail(teamId, email)) {
            throw new SecurityException("Bu takıma erişim yetkiniz yok.");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task bulunamadı: " + taskId));
        if (!task.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("Task bu takıma ait değil.");
        }
        return task;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

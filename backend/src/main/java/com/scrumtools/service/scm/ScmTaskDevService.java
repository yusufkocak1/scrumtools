package com.scrumtools.service.scm;

import com.scrumtools.dto.ScmBranchCreateRequest;
import com.scrumtools.dto.ScmBranchResponse;
import com.scrumtools.dto.ScmCommitResponse;
import com.scrumtools.dto.ScmPullRequestCreateRequest;
import com.scrumtools.dto.ScmPullRequestResponse;
import com.scrumtools.dto.ScmRepositoryResponse;
import com.scrumtools.dto.TaskScmResponse;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.ScmBranch;
import com.scrumtools.entity.ScmConnection;
import com.scrumtools.entity.ScmPullRequest;
import com.scrumtools.entity.ScmRepository;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.UserScmAccount;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.ScmBranchStatus;
import com.scrumtools.entity.enums.ScmConnectionStatus;
import com.scrumtools.entity.enums.ScmProvider;
import com.scrumtools.entity.enums.ScmPullRequestState;
import com.scrumtools.repository.ScmBranchRepository;
import com.scrumtools.repository.ScmCommitRepository;
import com.scrumtools.repository.ScmPullRequestRepository;
import com.scrumtools.repository.ScmRepositoryRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.repository.UserScmAccountRepository;
import com.scrumtools.service.AuditService;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.PermissionService;
import com.scrumtools.service.scm.client.ScmApiException;
import com.scrumtools.service.scm.client.ScmBranchInfo;
import com.scrumtools.service.scm.client.ScmClient;
import com.scrumtools.service.scm.client.ScmClientFactory;
import com.scrumtools.service.scm.client.ScmPullRequestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Task detayındaki Geliştirme (Dev) paneli verisini toplar.
 * Görüntüleme takım üyeliğiyle serbesttir (downgrade sonrası veri kaybolmaz);
 * yazma işlemleri featureEnabled + ilgili izni ister: branch açma SCM_CREATE_BRANCH,
 * pull request açma SCM_CREATE_PULL_REQUEST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScmTaskDevService {

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ScmRepositoryRepository scmRepositoryRepository;
    private final ScmBranchRepository scmBranchRepository;
    private final ScmPullRequestRepository scmPullRequestRepository;
    private final ScmCommitRepository scmCommitRepository;
    private final UserScmAccountRepository userScmAccountRepository;
    private final PermissionService permissionService;
    private final EntitlementService entitlementService;
    private final ScmClientFactory clientFactory;
    private final AuditService auditService;

    /** PR açıklamasına eklenen görev linki için (e-postalardakiyle aynı adres). */
    @Value("${app.frontend-base-url:}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public TaskScmResponse getTaskScm(UUID teamId, UUID taskId, String email) {
        Task task = getTaskInTeam(teamId, taskId, email);

        boolean featureEnabled = task.getTeam().getOrganization() != null
                && entitlementService.getEntitlements(task.getTeam().getOrganization().getId())
                        .features().contains(PlanFeature.GIT_INTEGRATION);

        // Repo eşleşmesi görevin projesine bağlı — takım çok projede çalışabildiği için
        // takımın birincil projesi değil, görevin kendi projesi kullanılır.
        Project project = task.getProject();
        if (project == null) {
            // Görev hiçbir projeye bağlı değil — DevPanel boş durum gösterir (§13)
            return new TaskScmResponse(featureEnabled, false, null,
                    false, false, false, false, List.of(), List.of(), List.of(), List.of());
        }

        List<ScmRepository> repos = scmRepositoryRepository.findByProjectId(project.getId());

        boolean canCreateBranch = featureEnabled && !repos.isEmpty()
                && permissionService.hasProjectPermission(email, project.getId(), Permission.SCM_CREATE_BRANCH);
        boolean canCreatePullRequest = featureEnabled && !repos.isEmpty()
                && permissionService.hasProjectPermission(email, project.getId(), Permission.SCM_CREATE_PULL_REQUEST);
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
                canCreatePullRequest,
                canManageRepos,
                hasUserAccount,
                repos.stream().map(ScmRepositoryResponse::from).toList(),
                scmBranchRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                        .map(ScmBranchResponse::from).toList(),
                scmPullRequestRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                        .map(ScmPullRequestResponse::from).toList(),
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
        Project project = task.getProject();
        if (project == null) {
            throw new IllegalStateException("Görev bir projeye bağlı değil.");
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

        ScmBranchInfo info = onProvider(repo, email, client -> client.createBranch(repo, branchName, sourceRef));

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
     * Task'a bağlı bir branch'ten sağlayıcıda pull request (GitLab'da merge request)
     * açar ve task'a bağlar. Branch açmadaki gibi önce kullanıcının kendi tokenı
     * denenir — PR sağlayıcıda kullanıcının adına görünür.
     */
    @Transactional
    public ScmPullRequestResponse createPullRequest(UUID teamId, UUID taskId, String email,
                                                    ScmPullRequestCreateRequest request) {
        Task task = getTaskInTeam(teamId, taskId, email);
        Project project = task.getProject();
        if (project == null) {
            throw new IllegalStateException("Görev bir projeye bağlı değil.");
        }
        if (task.getTeam().getOrganization() == null) {
            throw new IllegalStateException("Takım bir organizasyona bağlı değil.");
        }
        entitlementService.assertFeature(task.getTeam().getOrganization(), PlanFeature.GIT_INTEGRATION);
        permissionService.checkProjectPermission(email, project.getId(), Permission.SCM_CREATE_PULL_REQUEST);

        if (request.branchId() == null) {
            throw new IllegalArgumentException("Branch seçilmeli.");
        }
        ScmBranch branch = scmBranchRepository.findById(request.branchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch bulunamadı."));
        if (!branch.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException("Branch bu görevle ilişkili değil.");
        }
        if (branch.getStatus() == ScmBranchStatus.DELETED) {
            throw new IllegalStateException("Silinmiş branch için pull request açılamaz.");
        }

        ScmRepository repo = branch.getRepository();
        if (!repo.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Branch'in reposu bu projeye eşlenmiş değil.");
        }

        String targetBranch = isBlank(request.targetBranch())
                ? repo.getDefaultBranch() : request.targetBranch().trim();
        if (isBlank(targetBranch)) {
            throw new IllegalArgumentException("Hedef branch belirtilmeli.");
        }
        if (targetBranch.equals(branch.getName())) {
            throw new IllegalArgumentException("Hedef branch kaynak branch'ten farklı olmalı.");
        }

        // Aynı branch için zaten açık bir PR varsa yenisi açılmaz — kullanıcı mevcut olana yönlendirilir
        scmPullRequestRepository.findByBranchIdAndState(branch.getId(), ScmPullRequestState.OPEN).stream()
                .filter(pr -> pr.getTargetBranch().equals(targetBranch))
                .findFirst()
                .ifPresent(pr -> {
                    throw new IllegalStateException("Bu branch için zaten açık bir pull request var: #"
                            + pr.getExternalId());
                });

        String title = isBlank(request.title()) ? defaultTitle(task) : request.title().trim();
        String description = isBlank(request.description()) ? defaultDescription(task) : request.description();

        ScmPullRequestInfo info = createPullRequestOnProvider(
                repo, email, branch.getName(), targetBranch, title, description, request.draft());

        ScmPullRequest pullRequest = ScmPullRequest.builder()
                .repository(repo)
                .task(task)
                .branch(branch)
                .externalId(info.externalId())
                .title(info.title() != null ? info.title() : title)
                .sourceBranch(info.sourceBranch() != null ? info.sourceBranch() : branch.getName())
                .targetBranch(info.targetBranch() != null ? info.targetBranch() : targetBranch)
                .state(info.state() != null ? info.state() : ScmPullRequestState.OPEN)
                .webUrl(info.webUrl())
                .draft(info.draft())
                .createdViaApp(true)
                .createdBy(email)
                .mergedAt(info.mergedAt())
                .lastSyncedAt(LocalDateTime.now())
                .build();
        pullRequest = scmPullRequestRepository.save(pullRequest);

        auditService.recordChange(task, "pull request", null,
                "#" + pullRequest.getExternalId() + " " + pullRequest.getTitle(), email);
        log.info("Pull request açıldı: {} #{} ({} → {}, task={})", repo.getFullName(),
                pullRequest.getExternalId(), branch.getName(), targetBranch, task.getCustomId());
        return ScmPullRequestResponse.from(pullRequest);
    }

    /**
     * PR'ın sağlayıcıdaki güncel durumunu çeker ve kaydı günceller. PR merge
     * edildiyse kaynak branch de MERGED işaretlenir (push webhook'u bunu görmez).
     * Görüntüleme gibi takım üyeliğiyle serbesttir — dışarı tek bir okuma isteği çıkar.
     */
    @Transactional
    public ScmPullRequestResponse refreshPullRequest(UUID teamId, UUID taskId, UUID pullRequestId, String email) {
        Task task = getTaskInTeam(teamId, taskId, email);
        ScmPullRequest pullRequest = scmPullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Pull request bulunamadı."));
        if (!pullRequest.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException("Pull request bu görevle ilişkili değil.");
        }
        if (task.getTeam().getOrganization() == null) {
            throw new IllegalStateException("Takım bir organizasyona bağlı değil.");
        }
        entitlementService.assertFeature(task.getTeam().getOrganization(), PlanFeature.GIT_INTEGRATION);

        ScmRepository repo = pullRequest.getRepository();
        ScmPullRequestInfo info = onProvider(repo, email,
                client -> client.getPullRequest(repo, pullRequest.getExternalId()));

        if (info.title() != null) pullRequest.setTitle(info.title());
        if (info.targetBranch() != null) pullRequest.setTargetBranch(info.targetBranch());
        if (info.webUrl() != null) pullRequest.setWebUrl(info.webUrl());
        if (info.state() != null) pullRequest.setState(info.state());
        pullRequest.setDraft(info.draft());
        pullRequest.setMergedAt(info.mergedAt());
        pullRequest.setLastSyncedAt(LocalDateTime.now());
        scmPullRequestRepository.save(pullRequest);

        ScmBranch branch = pullRequest.getBranch();
        if (info.state() == ScmPullRequestState.MERGED
                && branch != null && branch.getStatus() == ScmBranchStatus.ACTIVE) {
            branch.setStatus(ScmBranchStatus.MERGED);
            scmBranchRepository.save(branch);
        }
        return ScmPullRequestResponse.from(pullRequest);
    }

    /**
     * PR'ı sağlayıcıda açar. Sağlayıcı "zaten var" derse (GitHub 422 / GitLab 409)
     * açık PR sorgulanıp döndürülür — kullanıcı hata yerine mevcut PR'ın linkini alır.
     */
    private ScmPullRequestInfo createPullRequestOnProvider(ScmRepository repo, String email,
                                                           String sourceBranch, String targetBranch,
                                                           String title, String description, boolean draft) {
        try {
            return onProvider(repo, email, client ->
                    client.createPullRequest(repo, sourceBranch, targetBranch, title, description, draft));
        } catch (ScmApiException e) {
            if (e.getStatusCode() != 409 && e.getStatusCode() != 422) throw e;
            ScmPullRequestInfo existing = onProvider(repo, email,
                    client -> client.findOpenPullRequest(repo, sourceBranch, targetBranch));
            if (existing == null) throw e;
            log.info("Sağlayıcıda zaten açık PR bulundu, mevcut kayıt bağlanıyor: {} #{}",
                    repo.getFullName(), existing.externalId());
            return existing;
        }
    }

    /**
     * Sağlayıcı çağrısını, varsa kullanıcının kendi tokenıyla çalıştırır; token
     * geçersiz çıkarsa hesap TOKEN_INVALID işaretlenir ve org bağlantısının
     * tokenıyla bir kez daha denenir.
     */
    private <T> T onProvider(ScmRepository repo, String email, Function<ScmClient, T> action) {
        ScmConnection connection = repo.getConnection();
        UserScmAccount userAccount = userScmAccountRepository
                .findByUserEmailAndProvider(email, connection.getProvider()).stream()
                .filter(a -> a.getStatus() == ScmConnectionStatus.ACTIVE)
                .filter(a -> Objects.equals(a.getBaseUrl(), connection.getBaseUrl()))
                .findFirst()
                .orElse(null);

        if (userAccount == null) {
            return action.apply(clientFactory.forConnection(connection));
        }
        try {
            return action.apply(clientFactory.forUserAccount(userAccount));
        } catch (ScmApiException e) {
            if (!e.isAuthFailure()) throw e;
            userAccount.setStatus(ScmConnectionStatus.TOKEN_INVALID);
            userScmAccountRepository.save(userAccount);
            log.warn("Kişisel SCM tokenı geçersiz ({}), org bağlantısına düşülüyor.", email);
            return action.apply(clientFactory.forConnection(connection));
        }
    }

    /** "DEV-19 Login sayfası hatası" — sağlayıcıda görev anahtarı başta görünsün. */
    private String defaultTitle(Task task) {
        String key = task.getCustomId();
        String title = task.getTitle() == null ? "" : task.getTitle().trim();
        String combined = isBlank(key) ? title : (key + " " + title).trim();
        return combined.length() > 500 ? combined.substring(0, 500) : combined;
    }

    /** Açıklamaya görev linki eklenir; frontend adresi tanımlı değilse sadece anahtar yazılır. */
    private String defaultDescription(Task task) {
        String key = task.getCustomId() == null ? "" : task.getCustomId();
        if (isBlank(frontendBaseUrl)) {
            return isBlank(key) ? "" : "ScrumTools görevi: " + key;
        }
        String link = frontendBaseUrl.replaceAll("/+$", "") + "/task/" + task.getId();
        return "ScrumTools görevi: [" + (isBlank(key) ? "Görev" : key) + "](" + link + ")";
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

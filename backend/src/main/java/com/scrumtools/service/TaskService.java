package com.scrumtools.service;

import com.scrumtools.dto.TaskRequest;
import com.scrumtools.dto.TaskResponse;
import com.scrumtools.dto.TaskSearchResponse;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.ActivityAction;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int SEARCH_RESULT_LIMIT = 20;

    private final TaskRepository taskRepository;
    private final TaskCommentRepository commentRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseService releaseService;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ActivityService activityService;

    // ─── Tasks ────────────────────────────────────────────────────────────────

    /**
     * Takımın görevleri. projectId verilirse yalnızca o projenin görevleri döner —
     * takım birden fazla projede çalışabildiği için aktif proje context'i bu filtreyi
     * kullanır; null geçilirse takımın tüm projelerindeki görevler döner ("Tüm projeler").
     */
    public List<TaskResponse> getTasksByTeam(UUID teamId, UUID projectId, boolean includeCancelled) {
        List<Task> tasks;
        if (projectId != null) {
            tasks = includeCancelled
                    ? taskRepository.findByTeamIdAndProjectId(teamId, projectId)
                    : taskRepository.findByTeamIdAndProjectIdAndStatusNot(teamId, projectId, "Cancelled");
        } else {
            tasks = includeCancelled
                    ? taskRepository.findByTeamId(teamId)
                    : taskRepository.findByTeamIdAndStatusNot(teamId, "Cancelled");
        }
        return tasks.stream().map(TaskResponse::from).collect(Collectors.toList());
    }

    public TaskResponse getTask(UUID teamId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getTeam().getId().equals(teamId)) throw new RuntimeException("Task does not belong to team");
        return TaskResponse.from(task);
    }

    /** Subtask'ları listele */
    @Transactional(readOnly = true)
    public List<TaskResponse> getSubtasks(UUID teamId, UUID parentTaskId) {
        Task parent = taskRepository.findById(parentTaskId)
                .orElseThrow(() -> new IllegalArgumentException("Task bulunamadı: " + parentTaskId));
        if (!parent.getTeam().getId().equals(teamId)) throw new IllegalArgumentException("Task bu takıma ait değil.");
        return parent.getSubtasks().stream().map(TaskResponse::from).toList();
    }

    /**
     * customId veya UUID ile cross-team arama — kullanıcının üye olduğu takımlarda arar.
     * UUID formatı algılanırsa önce id ile arar, bulamazsa customId ile dener.
     */
    public Optional<TaskResponse> findByCustomId(String customId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        List<UUID> teamIds = teamMemberRepository.findByEmail(userEmail)
                .stream()
                .map(tm -> tm.getTeam().getId())
                .collect(Collectors.toList());

        if (teamIds.isEmpty()) return Optional.empty();

        // UUID formatı kontrolü — UUID ile geldiyse önce id ile ara
        try {
            UUID taskUuid = UUID.fromString(customId);
            Optional<Task> byId = taskRepository.findById(taskUuid);
            if (byId.isPresent() && teamIds.contains(byId.get().getTeam().getId())) {
                return byId.map(TaskResponse::from);
            }
        } catch (IllegalArgumentException ignored) {
            // UUID formatında değil — customId olarak devam et
        }

        return taskRepository.findByCustomIdInTeams(customId, teamIds)
                .map(TaskResponse::from);
    }

    /** Takım içi typeahead arama — customId veya başlık ile (autocomplete picker için). */
    @Transactional(readOnly = true)
    public List<TaskSearchResponse> searchTasks(UUID teamId, String query) {
        String q = query == null ? "" : query.trim();
        return taskRepository.searchByCustomIdOrTitle(teamId, q, PageRequest.of(0, SEARCH_RESULT_LIMIT))
                .stream().map(TaskSearchResponse::from).toList();
    }

    /**
     * Görevin projesini belirler. Takım birden fazla projede çalışabildiği için proje
     * istekle birlikte gelir; gelmezse takımın birincil projesine, o da yoksa takımın
     * ilk projesine düşer. Hiç projesi olmayan takımlar için null döner — bu görevler
     * eski TEAMCODE-N isimlendirmesinde kalır.
     */
    private Project resolveTaskProject(Team team, String requestedProjectId) {
        if (requestedProjectId != null && !requestedProjectId.isBlank()) {
            UUID projectId = UUID.fromString(requestedProjectId.trim());
            return team.getProjects().stream()
                    .filter(p -> p.getId().equals(projectId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Seçilen proje bu takımın çalıştığı projeler arasında değil."));
        }
        if (team.getProject() != null) return team.getProject();
        return team.getProjects().stream().findFirst().orElse(null);
    }

    @Transactional
    public TaskResponse createTask(UUID teamId, TaskRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // customId proje bazlı üretilir: PROJEKEY-N (sayaç projede, aynı projedeki tüm
        // takımlar ortak numara alır). Takım hiçbir projeye bağlı değilse eski TEAMCODE-N
        // davranışına düşülür. Sayaç güncellemesi race condition yaratmasın diye ilgili
        // satır (proje ya da takım) kilitli okunur; sayaç boşsa mevcut customId'lerin
        // en büyük sonekinden başlatılır.
        Project target = resolveTaskProject(team, req.getProjectId());
        Project project = null;
        String customId;
        if (target != null) {
            project = projectRepository.findByIdForUpdate(target.getId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            Long sequence = project.getTaskSequence();
            if (sequence == null) {
                sequence = maxCustomIdSuffixForProject(project);
            }
            sequence = sequence + 1;
            project.setTaskSequence(sequence);
            customId = project.getKey() + "-" + sequence;
        } else {
            team = teamRepository.findByIdForUpdate(teamId)
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            Long sequence = team.getTaskSequence();
            if (sequence == null) {
                sequence = maxCustomIdSuffix(teamId);
            }
            sequence = sequence + 1;
            team.setTaskSequence(sequence);
            customId = team.getTeamCode() + "-" + sequence;
        }

        Sprint sprint = null;
        if (req.getSprintId() != null && !req.getSprintId().isBlank()) {
            sprint = sprintRepository.findById(UUID.fromString(req.getSprintId())).orElse(null);
        }

        Release release = null;
        if (req.getReleaseId() != null && !req.getReleaseId().isBlank()) {
            release = releaseRepository.findById(UUID.fromString(req.getReleaseId()))
                    .orElseThrow(() -> new RuntimeException("Release not found"));
            releaseService.validateTaskLink(release, project, userEmail);
        }

        Task parentTask = null;
        if (req.getParentTaskId() != null && !req.getParentTaskId().isBlank()) {
            parentTask = resolveParentTask(teamId, req.getParentTaskId().trim(), null);
        }

        Task task = Task.builder()
                .team(team)
                .project(project)
                .sprint(sprint)
                .release(release)
                .parentTask(parentTask)
                .customId(customId)
                .title(req.getTitle())
                .description(req.getDescription())
                .status(req.getStatus() != null ? req.getStatus() : "To Do")
                .issueType(req.getIssueType() != null ? req.getIssueType() : "task")
                .priority(req.getPriority() != null ? req.getPriority() : "Medium")
                .reporter(req.getReporter() != null ? req.getReporter() : userEmail)
                .assignee(req.getAssignee())
                .developer(req.getDeveloper())
                .analyst(req.getAnalyst())
                .tester(req.getTester())
                .storyPoints(req.getStoryPoints())
                .labels(req.getLabels() != null ? req.getLabels() : new ArrayList<>())
                .dueDate(req.getDueDate())
                .startDate(req.getStartDate())
                .estimatedHours(req.getEstimatedHours())
                .environment(req.getEnvironment())
                .position(req.getPosition() != null ? req.getPosition() : 0)
                .customFields(req.getCustomFields() != null ? req.getCustomFields() : new HashMap<>())
                .build();

        Task saved = taskRepository.save(task);

        // Bildirim: atanan kişiyi bilgilendir
        if (saved.getAssignee() != null && !saved.getAssignee().equals(userEmail)) {
            notificationService.notifyTaskAssigned(
                    saved.getAssignee(),
                    saved.getCustomId(),
                    saved.getTitle(),
                    saved.getTeam().getId().toString(),
                    saved.getId().toString()
            );
        }

        // Aktivite: task oluşturuldu
        Map<String, Object> actDetails = new HashMap<>();
        actDetails.put("customId", saved.getCustomId());
        actDetails.put("title", saved.getTitle());
        activityService.record(userEmail, ActivityAction.TASK_CREATED,
                "task", saved.getId().toString(), saved.getTeam().getId(), actDetails);

        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse updateTask(UUID teamId, UUID taskId, TaskRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getTeam().getId().equals(teamId)) throw new RuntimeException("Task does not belong to team");

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // Audit: izlenen alanları kaydet
        String oldStatus = task.getStatus();
        String oldAssignee = task.getAssignee();

        if (req.getStatus() != null) {
            auditService.recordChange(task, "status", task.getStatus(), req.getStatus(), userEmail);
            task.setStatus(req.getStatus());
            if (isDoneStatus(req.getStatus()) && task.getResolvedAt() == null) {
                task.setResolvedAt(LocalDateTime.now());
            }
        }
        if (req.getPriority() != null) {
            auditService.recordChange(task, "priority", task.getPriority(), req.getPriority(), userEmail);
            task.setPriority(req.getPriority());
        }
        if (req.getAssignee() != null) {
            auditService.recordChange(task, "assignee", task.getAssignee(), req.getAssignee(), userEmail);
            task.setAssignee(req.getAssignee());
        }
        if (req.getTitle() != null) {
            auditService.recordChange(task, "title", task.getTitle(), req.getTitle(), userEmail);
            task.setTitle(req.getTitle());
        }

        // Audit olmayan güncellemeler
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getIssueType() != null) task.setIssueType(req.getIssueType());
        if (req.getDeveloper() != null) task.setDeveloper(req.getDeveloper());
        if (req.getAnalyst() != null) task.setAnalyst(req.getAnalyst());
        if (req.getTester() != null) task.setTester(req.getTester());
        if (req.getStoryPoints() != null) task.setStoryPoints(req.getStoryPoints());
        if (req.getLabels() != null) task.setLabels(req.getLabels());
        if (req.getDueDate() != null) task.setDueDate(req.getDueDate());
        if (req.getStartDate() != null) task.setStartDate(req.getStartDate());
        if (req.getEstimatedHours() != null) task.setEstimatedHours(req.getEstimatedHours());
        if (req.getLoggedHours() != null) task.setLoggedHours(req.getLoggedHours());
        if (req.getEnvironment() != null) task.setEnvironment(req.getEnvironment());
        if (req.getResolution() != null) task.setResolution(req.getResolution());
        if (req.getPosition() != null) task.setPosition(req.getPosition());

        if (req.getCustomFields() != null) {
            Map<String, Object> merged = new HashMap<>(task.getCustomFields());
            merged.putAll(req.getCustomFields());
            task.setCustomFields(merged);
        }

        if (req.getSprintId() != null) {
            if (req.getSprintId().isBlank()) {
                task.setSprint(null);
            } else {
                Sprint sprint = sprintRepository.findById(UUID.fromString(req.getSprintId()))
                        .orElseThrow(() -> new RuntimeException("Sprint not found"));
                task.setSprint(sprint);
            }
        }

        // Proje değişikliği — release bağından önce işlenir ki release doğrulaması
        // görevin yeni projesine göre yapılsın. customId bilinçli olarak korunur:
        // git branch eşleştirmeleri ve dış linkler eski anahtara bağlı.
        if (req.getProjectId() != null && !req.getProjectId().isBlank()) {
            UUID newProjectId = UUID.fromString(req.getProjectId().trim());
            Project current = task.getProject();
            if (current == null || !current.getId().equals(newProjectId)) {
                Project newProject = resolveTaskProject(task.getTeam(), req.getProjectId());
                auditService.recordChange(task, "project",
                        current != null ? current.getKey() : null, newProject.getKey(), userEmail);
                task.setProject(newProject);

                // Sürüm proje seviyesinde — yeni projede geçersiz kalan bağ temizlenir.
                Release staleRelease = task.getRelease();
                if (staleRelease != null && !staleRelease.getProject().getId().equals(newProject.getId())) {
                    releaseService.validateTaskUnlink(staleRelease, userEmail);
                    auditService.recordChange(task, "release", staleRelease.getName(), null, userEmail);
                    task.setRelease(null);
                }
            }
        }

        if (req.getReleaseId() != null) {
            Release oldRelease = task.getRelease();
            if (req.getReleaseId().isBlank()) {
                if (oldRelease != null) {
                    releaseService.validateTaskUnlink(oldRelease, userEmail);
                    auditService.recordChange(task, "release", oldRelease.getName(), null, userEmail);
                    task.setRelease(null);
                }
            } else {
                Release newRelease = releaseRepository.findById(UUID.fromString(req.getReleaseId()))
                        .orElseThrow(() -> new RuntimeException("Release not found"));
                if (oldRelease == null || !oldRelease.getId().equals(newRelease.getId())) {
                    if (oldRelease != null) releaseService.validateTaskUnlink(oldRelease, userEmail);
                    releaseService.validateTaskLink(newRelease, task.getProject(), userEmail);
                    auditService.recordChange(task, "release",
                            oldRelease != null ? oldRelease.getName() : null, newRelease.getName(), userEmail);
                    task.setRelease(newRelease);
                }
            }
        }

        if (req.getParentTaskId() != null) {
            if (req.getParentTaskId().isBlank()) {
                task.setParentTask(null);
            } else {
                if (task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
                    throw new IllegalArgumentException("Alt görevleri olan bir görev başka görevin altına taşınamaz.");
                }
                task.setParentTask(resolveParentTask(teamId, req.getParentTaskId().trim(), task.getId()));
            }
        }

        Task updated = taskRepository.save(task);

        // ─── Post-save bildirimler ───────────────────────────────────────────
        String updatedTeamId = updated.getTeam().getId().toString();
        String updatedTaskId = updated.getId().toString();

        // Assignee değişmişse yeni atanan kişiyi bilgilendir
        if (req.getAssignee() != null && !req.getAssignee().equals(oldAssignee)
                && !req.getAssignee().isBlank() && !req.getAssignee().equals(userEmail)) {
            notificationService.notifyTaskAssigned(req.getAssignee(),
                    updated.getCustomId(), updated.getTitle(), updatedTeamId, updatedTaskId);
        }

        // Status değiştiyse assignee'yi bilgilendir (değiştiren kişi değilse)
        if (req.getStatus() != null && !req.getStatus().equals(oldStatus)) {
            String notifyTarget = updated.getAssignee();
            if (notifyTarget != null && !notifyTarget.isBlank() && !notifyTarget.equals(userEmail)) {
                notificationService.notifyTaskStatusChanged(notifyTarget, userEmail,
                        updated.getCustomId(), updated.getTitle(),
                        oldStatus, req.getStatus(), updatedTeamId, updatedTaskId);
            }
            // Watcherları da bilgilendir
            for (String watcher : updated.getWatchers()) {
                if (!watcher.equals(userEmail) && !watcher.equals(notifyTarget)) {
                    notificationService.notifyTaskStatusChanged(watcher, userEmail,
                            updated.getCustomId(), updated.getTitle(),
                            oldStatus, req.getStatus(), updatedTeamId, updatedTaskId);
                }
            }
        }

        // Watcherlar için diğer değişiklikler
        String changedField = req.getPriority() != null ? "priority"
                : req.getTitle() != null ? "title" : null;
        if (changedField != null) {
            for (String watcher : updated.getWatchers()) {
                if (!watcher.equals(userEmail)) {
                    notificationService.notifyWatchedTaskUpdated(watcher, userEmail,
                            updated.getCustomId(), updated.getTitle(),
                            changedField, updatedTeamId, updatedTaskId);
                }
            }
        }

        // Aktivite: task güncellendi
        Map<String, Object> actDetails = new HashMap<>();
        actDetails.put("customId", updated.getCustomId());
        actDetails.put("title", updated.getTitle());
        activityService.record(userEmail, ActivityAction.TASK_UPDATED,
                "task", updatedTaskId, updated.getTeam().getId(), actDetails);

        return TaskResponse.from(updated);
    }

    @Transactional
    public TaskResponse updateStatus(UUID teamId, UUID taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getTeam().getId().equals(teamId)) throw new RuntimeException("Task does not belong to team");

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.recordChange(task, "status", task.getStatus(), status, userEmail);
        task.setStatus(status);
        if (isDoneStatus(status) && task.getResolvedAt() == null) {
            task.setResolvedAt(LocalDateTime.now());
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(UUID teamId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getTeam().getId().equals(teamId)) throw new RuntimeException("Task does not belong to team");
        taskRepository.delete(task);
    }

    // ─── Watcher Management ───────────────────────────────────────────────────

    @Transactional
    public TaskResponse addWatcher(UUID teamId, UUID taskId, String watcherEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task bulunamadı: " + taskId));
        if (!task.getTeam().getId().equals(teamId)) throw new IllegalArgumentException("Task bu takıma ait değil.");
        if (!task.getWatchers().contains(watcherEmail)) {
            task.getWatchers().add(watcherEmail);
            taskRepository.save(task);
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse removeWatcher(UUID teamId, UUID taskId, String watcherEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task bulunamadı: " + taskId));
        if (!task.getTeam().getId().equals(teamId)) throw new IllegalArgumentException("Task bu takıma ait değil.");
        task.getWatchers().remove(watcherEmail);
        taskRepository.save(task);
        return TaskResponse.from(task);
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    @Transactional
    public TaskResponse addComment(UUID teamId, UUID taskId, String text) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getTeam().getId().equals(teamId)) throw new RuntimeException("Task does not belong to team");

        String author = SecurityContextHolder.getContext().getAuthentication().getName();

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(author)
                .text(text)
                .build();

        commentRepository.save(comment);
        task.getComments().add(comment);

        // Bildirim: assignee ve watcherlara bildir
        String commentTaskId = task.getId().toString();
        String commentTeamId = task.getTeam().getId().toString();
        Set<String> toNotify = new HashSet<>(task.getWatchers());
        if (task.getAssignee() != null) toNotify.add(task.getAssignee());
        if (task.getReporter() != null) toNotify.add(task.getReporter());
        toNotify.remove(author); // yorumu yapan kişiyi çıkar
        for (String recipient : toNotify) {
            notificationService.notifyTaskCommented(recipient, author,
                    task.getCustomId(), task.getTitle(), commentTeamId, commentTaskId);
        }

        // Aktivite
        Map<String, Object> actDetails = new HashMap<>();
        actDetails.put("customId", task.getCustomId());
        activityService.record(author, ActivityAction.TASK_COMMENTED,
                "task", commentTaskId, task.getTeam().getId(), actDetails);

        return TaskResponse.from(task);
    }

    @Transactional
    public void deleteComment(@SuppressWarnings("unused") UUID teamId, UUID taskId, UUID commentId) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getTask().getId().equals(taskId)) throw new RuntimeException("Comment does not belong to task");
        commentRepository.delete(comment);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isDoneStatus(String status) {
        return "Done".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status)
                || "Fixed".equalsIgnoreCase(status) || "Verified".equalsIgnoreCase(status);
    }

    /**
     * Parent task'ı UUID veya customId ile çözer; bulunamazsa hata fırlatır
     * (eski davranış geçersiz parent'ı sessizce düşürüyordu).
     * Jira benzeri tek seviye hiyerarşi: subtask'ın altına görev eklenemez.
     */
    private Task resolveParentTask(UUID teamId, String idOrCustomId, UUID childTaskId) {
        Task parent = null;
        try {
            parent = taskRepository.findById(UUID.fromString(idOrCustomId)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            // UUID formatında değil — customId olarak dene
        }
        if (parent == null) {
            parent = taskRepository.findByTeamIdAndCustomId(teamId, idOrCustomId)
                    .orElseThrow(() -> new IllegalArgumentException("Üst görev bulunamadı: " + idOrCustomId));
        }
        if (!parent.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("Üst görev bu takıma ait değil.");
        }
        if (childTaskId != null && parent.getId().equals(childTaskId)) {
            throw new IllegalArgumentException("Bir görev kendisinin üst görevi olamaz.");
        }
        if (parent.getParentTask() != null) {
            throw new IllegalArgumentException("Bir alt görevin altına görev eklenemez.");
        }
        return parent;
    }

    /** Takımdaki mevcut customId'lerin en büyük sayısal soneki (sayaç başlatma için). */
    private long maxCustomIdSuffix(UUID teamId) {
        return maxSuffix(taskRepository.findCustomIdsByTeamId(teamId), null);
    }

    /**
     * Projedeki mevcut customId'lerin en büyük sayısal soneki (proje sayacı başlatma için).
     * Sadece proje anahtarıyla başlayan ID'ler sayılır — takım kodu proje anahtarından
     * farklıysa eski TEAMCODE-N görevleri numaralandırmayı etkilemez.
     */
    private long maxCustomIdSuffixForProject(Project project) {
        return maxSuffix(taskRepository.findCustomIdsByProjectId(project.getId()), project.getKey() + "-");
    }

    private long maxSuffix(List<String> customIds, String requiredPrefix) {
        return customIds.stream()
                .filter(cid -> requiredPrefix == null || cid.startsWith(requiredPrefix))
                .map(cid -> cid.substring(cid.lastIndexOf('-') + 1))
                .filter(s -> !s.isEmpty() && s.chars().allMatch(Character::isDigit))
                .mapToLong(Long::parseLong)
                .max().orElse(0L);
    }
}

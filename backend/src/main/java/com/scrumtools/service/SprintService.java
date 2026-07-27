package com.scrumtools.service;

import com.scrumtools.dto.CompleteSprintRequest;
import com.scrumtools.dto.CompleteSprintResponse;
import com.scrumtools.dto.SprintRequest;
import com.scrumtools.dto.SprintResponse;
import com.scrumtools.entity.Sprint;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.enums.ActivityAction;
import com.scrumtools.repository.SprintRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    /** Tamamlanmamış işlere uygulanabilecek aksiyonlar (CompleteSprintRequest). */
    private static final String ACTION_BACKLOG = "BACKLOG";
    private static final String ACTION_MOVE = "MOVE";
    private static final String ACTION_COMPLETE = "COMPLETE";
    private static final String ACTION_KEEP = "KEEP";

    /** COMPLETE aksiyonunda yarım kalan işlere yazılan status. */
    private static final String DONE_STATUS = "Done";

    private final SprintRepository sprintRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final AuditService auditService;
    private final ActivityService activityService;

    public List<SprintResponse> getSprintsByTeam(UUID teamId) {
        return sprintRepository.findByTeamId(teamId)
                .stream()
                .map(SprintResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SprintResponse createSprint(UUID teamId, SprintRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        Sprint sprint = Sprint.builder()
                .team(team)
                .name(req.getName())
                .goal(req.getGoal())
                .status("backlog")
                .startDate(parseDate(req.getStartDate()))
                .endDate(parseDate(req.getEndDate()))
                .build();

        return SprintResponse.from(sprintRepository.save(sprint));
    }

    @Transactional
    public SprintResponse updateSprint(UUID teamId, UUID sprintId, SprintRequest req) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));
        if (!sprint.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Sprint does not belong to team");
        }

        if (req.getName() != null) sprint.setName(req.getName());
        if (req.getGoal() != null) sprint.setGoal(req.getGoal());
        if (req.getStartDate() != null) sprint.setStartDate(parseDate(req.getStartDate()));
        if (req.getEndDate() != null) sprint.setEndDate(parseDate(req.getEndDate()));

        return SprintResponse.from(sprintRepository.save(sprint));
    }

    /**
     * Birden fazla sprint aynı anda aktif (open) olabilir — kısıtlama yok.
     */
    @Transactional
    public SprintResponse updateStatus(UUID teamId, UUID sprintId, String status) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));
        if (!sprint.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Sprint does not belong to team");
        }
        sprint.setStatus(status);

        // Sprint açılırken tarih boşsa otomatik bugünü ata
        if ("open".equals(status) && sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }

        return SprintResponse.from(sprintRepository.save(sprint));
    }

    /**
     * Sprinti kapatır ve tamamlanmamış işlerin akıbetini isteğe göre belirler.
     *
     * Eski davranış sprinti sadece "done" işaretliyordu: yarım kalan işler kapanan
     * sprintin içinde kalıyor, backlog ekranı kapalı sprintleri gizlediği için de
     * gözden kayboluyordu. Artık kapatan kişi ne olacağını seçer.
     */
    @Transactional
    public CompleteSprintResponse completeSprint(UUID teamId, UUID sprintId, CompleteSprintRequest req) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));
        if (!sprint.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Sprint does not belong to team");
        }

        String action = normalizeAction(req == null ? null : req.getIncompleteAction());
        Sprint target = action.equals(ACTION_MOVE)
                ? resolveTargetSprint(teamId, sprintId, req.getTargetSprintId())
                : null;

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // İptal edilen işler ne "tamamlandı" ne de "yarım kalmış" sayılır —
        // taşınmaları veya toplu kapatılmaları kullanıcıyı yanıltır.
        List<Task> tasks = taskRepository.findByTeamIdAndSprintId(teamId, sprintId).stream()
                .filter(t -> !"Cancelled".equalsIgnoreCase(t.getStatus()))
                .toList();

        List<Task> incomplete = tasks.stream()
                .filter(t -> !TaskService.isDoneStatus(t.getStatus()))
                .toList();

        for (Task task : incomplete) {
            switch (action) {
                case ACTION_BACKLOG -> task.setSprint(null);
                case ACTION_MOVE -> task.setSprint(target);
                case ACTION_COMPLETE -> {
                    auditService.recordChange(task, "status", task.getStatus(), DONE_STATUS, userEmail);
                    task.setStatus(DONE_STATUS);
                    if (task.getResolvedAt() == null) task.setResolvedAt(LocalDateTime.now());
                }
                default -> { /* KEEP — görev kapanan sprintte kalır */ }
            }
        }
        if (!action.equals(ACTION_KEEP)) {
            taskRepository.saveAll(incomplete);
        }

        sprint.setStatus("done");
        // Kapanış tarihi girilmemişse bugüne çekilir — velocity/burndown raporları
        // sprintin bittiği günü bu alandan okuyor (sprint açılışındaki startDate ile aynı mantık).
        if (sprint.getEndDate() == null) {
            sprint.setEndDate(LocalDate.now());
        }
        Sprint saved = sprintRepository.save(sprint);

        // "title" anahtarı bilinçli: aktivite akışı olayın alt satırında bu alanı
        // basıyor — sprint adı böylece feed'de görünür hâle gelir.
        Map<String, Object> details = new HashMap<>();
        details.put("title", saved.getName());
        details.put("completedCount", tasks.size() - incomplete.size());
        details.put("incompleteCount", incomplete.size());
        details.put("incompleteAction", action);
        if (target != null) details.put("targetSprintName", target.getName());
        activityService.record(userEmail, ActivityAction.SPRINT_COMPLETED,
                "sprint", saved.getId().toString(), teamId, details);

        return CompleteSprintResponse.builder()
                .sprint(SprintResponse.from(saved))
                .completedCount(tasks.size() - incomplete.size())
                .incompleteCount(incomplete.size())
                .incompleteAction(action)
                .targetSprintName(target != null ? target.getName() : null)
                .build();
    }

    /** Bilinmeyen/boş aksiyon backlog'a düşer — yarım işi sessizce kaybetmemek için en güvenli seçenek. */
    private String normalizeAction(String action) {
        if (action == null) return ACTION_BACKLOG;
        // Locale.ROOT: sunucu tr_TR locale'inde çalışırsa "i" → "İ" dönüşümü
        // aksiyon adlarını tanınmaz hâle getirebilir.
        String normalized = action.trim().toUpperCase(Locale.ROOT);
        return List.of(ACTION_BACKLOG, ACTION_MOVE, ACTION_COMPLETE, ACTION_KEEP).contains(normalized)
                ? normalized
                : ACTION_BACKLOG;
    }

    private Sprint resolveTargetSprint(UUID teamId, UUID sourceSprintId, String targetSprintId) {
        if (targetSprintId == null || targetSprintId.isBlank()) {
            throw new IllegalArgumentException("Taşınacak sprint seçilmedi.");
        }
        Sprint target = sprintRepository.findById(UUID.fromString(targetSprintId.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Hedef sprint bulunamadı."));
        if (!target.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("Hedef sprint bu takıma ait değil.");
        }
        if (target.getId().equals(sourceSprintId)) {
            throw new IllegalArgumentException("Görevler kapatılan sprintin kendisine taşınamaz.");
        }
        if ("done".equalsIgnoreCase(target.getStatus())) {
            throw new IllegalArgumentException("Görevler kapatılmış bir sprinte taşınamaz.");
        }
        return target;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return LocalDate.parse(dateStr);
    }
}

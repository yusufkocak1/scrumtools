package com.scrumtools.service;

import com.scrumtools.dto.report.*;
import com.scrumtools.entity.Sprint;
import com.scrumtools.entity.Task;
import com.scrumtools.repository.SprintRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final TeamMemberRepository teamMemberRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // ─── Team Summary ─────────────────────────────────────────────────────────

    public TeamSummaryDto getTeamSummary(UUID teamId) {
        List<Task> all = taskRepository.findByTeamId(teamId);
        long total = all.size();
        long open = all.stream().filter(t -> "To Do".equals(t.getStatus())).count();
        long inProgress = all.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
        long done = all.stream().filter(t -> "Done".equals(t.getStatus())).count();
        long overdue = taskRepository.findOverdue(teamId, LocalDate.now()).size();

        List<Sprint> sprints = sprintRepository.findByTeamId(teamId);
        long activeSprints = sprints.stream().filter(s -> "open".equals(s.getStatus())).count();

        List<DistributionItemDto> byPriority = toDistribution(taskRepository.countByPriority(teamId));
        List<DistributionItemDto> byType = toDistribution(taskRepository.countByIssueType(teamId));
        List<DistributionItemDto> byStatus = toDistribution(taskRepository.countByStatus(teamId));

        return new TeamSummaryDto(
                (int) total, (int) open, (int) inProgress, (int) done, (int) overdue,
                sprints.size(), (int) activeSprints,
                byPriority, byType, byStatus
        );
    }

    // ─── Burndown ─────────────────────────────────────────────────────────────

    public BurndownReportDto getBurndown(UUID teamId, UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IllegalArgumentException("Sprint bulunamadı: " + sprintId));
        if (!sprint.getTeam().getId().equals(teamId))
            throw new IllegalArgumentException("Sprint bu takıma ait değil.");

        List<Task> sprintTasks = taskRepository.findBySprintId(sprintId);

        int totalPoints = sprintTasks.stream()
                .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                .sum();

        // Sprint tarihleri null ise placeholder kullan
        LocalDate start = sprint.getStartDate() != null ? sprint.getStartDate() : LocalDate.now().minusDays(14);
        LocalDate end = sprint.getEndDate() != null ? sprint.getEndDate() : LocalDate.now();

        long daysTotal = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (daysTotal <= 0) daysTotal = 1;

        List<BurndownPointDto> points = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Her gün için ideal ve gerçek (basit yaklaşım: Done'a geçiş günü bilmiyoruz → güncel Done pts)
        int donePoints = sprintTasks.stream()
                .filter(t -> "Done".equals(t.getStatus()))
                .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                .sum();
        double dailyIdeal = (double) totalPoints / (daysTotal - 1 == 0 ? 1 : daysTotal - 1);

        for (long i = 0; i < daysTotal; i++) {
            LocalDate day = start.plusDays(i);
            double ideal = Math.max(0, totalPoints - dailyIdeal * i);
            // actual: sadece bugün ve öncesi için gerçek değer
            double actual;
            if (day.isAfter(today)) {
                actual = -1; // gelecek gün — frontend göstermesin
            } else if (i == daysTotal - 1 || !day.isBefore(today)) {
                // son nokta veya bugün
                actual = totalPoints - donePoints;
            } else {
                // geçmiş günler için ideal ile eşit tut (TaskHistory yoksa elimizde bilgi yok)
                actual = Math.max(0, totalPoints - dailyIdeal * i);
            }
            points.add(new BurndownPointDto(day.format(DATE_FMT), Math.round(ideal * 10) / 10.0, Math.round(actual * 10) / 10.0));
        }

        return new BurndownReportDto(
                sprintId.toString(), sprint.getName(),
                sprint.getStartDate(), sprint.getEndDate(),
                totalPoints, points
        );
    }

    // ─── Velocity ─────────────────────────────────────────────────────────────

    public List<SprintVelocityDto> getVelocity(UUID teamId) {
        List<Sprint> sprints = sprintRepository.findByTeamId(teamId);
        return sprints.stream().map(sprint -> {
            List<Task> tasks = taskRepository.findBySprintId(sprint.getId());
            int committed = tasks.stream()
                    .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                    .sum();
            int completed = tasks.stream()
                    .filter(t -> "Done".equals(t.getStatus()))
                    .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                    .sum();
            return new SprintVelocityDto(sprint.getId().toString(), sprint.getName(), committed, completed);
        }).collect(Collectors.toList());
    }

    // ─── Workload ─────────────────────────────────────────────────────────────

    public List<WorkloadDto> getWorkload(UUID teamId) {
        List<Object[]> rows = taskRepository.workloadByAssignee(teamId);

        // Display name'i TeamMember tablosundan çek
        Map<String, String> nameMap = teamMemberRepository.findByTeamId(teamId)
                .stream()
                .collect(Collectors.toMap(
                        tm -> tm.getEmail(),
                        tm -> tm.getDisplayName(),
                        (a, b) -> a
                ));

        return rows.stream().map(row -> {
            String email = (String) row[0];
            int taskCount = row[1] instanceof Number ? ((Number) row[1]).intValue() : 0;
            int storyPts = row[2] instanceof Number ? ((Number) row[2]).intValue() : 0;
            String name = nameMap.getOrDefault(email, email);
            return new WorkloadDto(email, name, taskCount, storyPts);
        }).collect(Collectors.toList());
    }

    // ─── Created vs Resolved ──────────────────────────────────────────────────

    public List<CreatedVsResolvedDto> getCreatedVsResolved(UUID teamId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        Map<String, Integer> createdMap = new LinkedHashMap<>();
        Map<String, Integer> resolvedMap = new LinkedHashMap<>();

        taskRepository.countCreatedPerDay(teamId, since)
                .forEach(row -> createdMap.put(row[0].toString(), ((Number) row[1]).intValue()));
        taskRepository.countResolvedPerDay(teamId, since)
                .forEach(row -> resolvedMap.put(row[0].toString(), ((Number) row[1]).intValue()));

        // Tüm tarihleri birleştir
        Set<String> allDates = new TreeSet<>();
        allDates.addAll(createdMap.keySet());
        allDates.addAll(resolvedMap.keySet());

        return allDates.stream()
                .map(d -> new CreatedVsResolvedDto(d,
                        createdMap.getOrDefault(d, 0),
                        resolvedMap.getOrDefault(d, 0)))
                .collect(Collectors.toList());
    }

    // ─── Overdue ──────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getOverdueTasks(UUID teamId) {
        return taskRepository.findOverdue(teamId, LocalDate.now())
                .stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("customId", t.getCustomId());
                    m.put("title", t.getTitle());
                    m.put("status", t.getStatus());
                    m.put("priority", t.getPriority());
                    m.put("assignee", t.getAssignee());
                    m.put("dueDate", t.getDueDate());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─── Yardımcı ─────────────────────────────────────────────────────────────

    private List<DistributionItemDto> toDistribution(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new DistributionItemDto(
                        r[0] != null ? r[0].toString() : "Unknown",
                        ((Number) r[1]).intValue()))
                .collect(Collectors.toList());
    }

    private LocalDate parseDate(String dateStr, LocalDate fallback) {
        if (dateStr == null || dateStr.isBlank()) return fallback;
        try { return LocalDate.parse(dateStr, DATE_FMT); }
        catch (Exception e) { return fallback; }
    }
}


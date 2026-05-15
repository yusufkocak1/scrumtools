package com.scrumtools.service;

import com.scrumtools.dto.SprintRequest;
import com.scrumtools.dto.SprintResponse;
import com.scrumtools.entity.Sprint;
import com.scrumtools.entity.Team;
import com.scrumtools.repository.SprintRepository;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TeamRepository teamRepository;

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

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return LocalDate.parse(dateStr);
    }
}

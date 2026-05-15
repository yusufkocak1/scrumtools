package com.scrumtools.service;

import com.scrumtools.dto.ActivityEventResponse;
import com.scrumtools.entity.ActivityEvent;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.enums.ActivityAction;
import com.scrumtools.repository.ActivityEventRepository;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityEventRepository activityEventRepository;
    private final TeamRepository teamRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Aktivite olayı kaydeder ve WebSocket üzerinden takıma push gönderir.
     */
    @Async
    @Transactional
    public void record(String actorEmail,
                       ActivityAction action,
                       String entityType,
                       String entityId,
                       UUID teamId,
                       Map<String, Object> details) {
        Team team = null;
        if (teamId != null) {
            team = teamRepository.findById(teamId).orElse(null);
        }

        ActivityEvent event = ActivityEvent.builder()
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .team(team)
                .details(details)
                .build();

        ActivityEvent saved = activityEventRepository.save(event);
        ActivityEventResponse dto = ActivityEventResponse.from(saved);

        // WebSocket push — takıma özel topic
        if (teamId != null) {
            try {
                messagingTemplate.convertAndSend("/topic/team/" + teamId + "/activity", dto);
            } catch (Exception ex) {
                log.warn("[Activity] WebSocket push başarısız (team={}): {}", teamId, ex.getMessage());
            }
        }
    }

    // ─── REST ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ActivityEventResponse> getTeamActivity(UUID teamId, int page, int size) {
        return activityEventRepository
                .findByTeamIdOrderByCreatedAtDesc(teamId, PageRequest.of(page, size))
                .map(ActivityEventResponse::from);
    }
}


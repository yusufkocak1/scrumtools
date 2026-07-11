package com.scrumtools.service;

import com.scrumtools.dto.TaskLinkRequest;
import com.scrumtools.dto.TaskLinkResponse;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.TaskLink;
import com.scrumtools.repository.TaskLinkRepository;
import com.scrumtools.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskLinkService {

    private final TaskLinkRepository taskLinkRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskLinkResponse> getLinks(UUID taskId) {
        return taskLinkRepository.findAllByTaskId(taskId).stream()
                .map(TaskLinkResponse::from).toList();
    }

    @Transactional
    public TaskLinkResponse createLink(UUID teamId, UUID taskId, TaskLinkRequest request) {
        Task sourceTask = findTask(teamId, taskId);
        Task targetTask = resolveTargetTask(teamId, request.targetTaskId());

        if (sourceTask.getId().equals(targetTask.getId())) {
            throw new IllegalArgumentException("Bir task kendisiyle ilişkilendirilemez.");
        }

        // Aynı ilişki zaten var mı?
        if (taskLinkRepository.existsBySourceTaskIdAndTargetTaskIdAndLinkType(
                taskId, targetTask.getId(), request.linkType())) {
            throw new IllegalArgumentException("Bu ilişki zaten mevcut.");
        }

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        TaskLink link = TaskLink.builder()
                .sourceTask(sourceTask)
                .targetTask(targetTask)
                .linkType(request.linkType())
                .createdBy(userEmail)
                .build();

        link = taskLinkRepository.save(link);
        log.info("Task ilişkisi oluşturuldu: {} {} {} (by={})",
                sourceTask.getCustomId(), request.linkType().getLabel(), targetTask.getCustomId(), userEmail);
        return TaskLinkResponse.from(link);
    }

    @Transactional
    public void deleteLink(UUID teamId, UUID taskId, UUID linkId) {
        findTask(teamId, taskId); // ownership check
        TaskLink link = taskLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("İlişki bulunamadı: " + linkId));
        taskLinkRepository.delete(link);
    }

    private Task findTask(UUID teamId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task bulunamadı: " + taskId));
        if (!task.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("Task bu takıma ait değil.");
        }
        return task;
    }

    /** Hedef task'ı UUID veya customId (örn. TEAM-5) ile çözer; customId takım kapsamlıdır. */
    private Task resolveTargetTask(UUID teamId, String idOrCustomId) {
        if (idOrCustomId == null || idOrCustomId.isBlank()) {
            throw new IllegalArgumentException("Hedef task belirtilmedi.");
        }
        String value = idOrCustomId.trim();
        try {
            Optional<Task> byId = taskRepository.findById(UUID.fromString(value));
            if (byId.isPresent()) return byId.get();
        } catch (IllegalArgumentException ignored) {
            // UUID formatında değil — customId olarak dene
        }
        return taskRepository.findByTeamIdAndCustomId(teamId, value)
                .orElseThrow(() -> new IllegalArgumentException("Hedef task bulunamadı: " + value));
    }
}


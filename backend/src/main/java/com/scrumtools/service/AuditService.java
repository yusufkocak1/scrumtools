package com.scrumtools.service;

import com.scrumtools.dto.TaskHistoryResponse;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.TaskHistory;
import com.scrumtools.repository.TaskHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Task değişikliklerini audit log olarak kaydeder.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final TaskHistoryRepository historyRepository;

    /**
     * Belirtilen alandaki değişikliği kaydet (oldValue == newValue ise kaydetme).
     */
    @Transactional
    public void recordChange(Task task, String field, String oldValue, String newValue, String changedBy) {
        if (Objects.equals(oldValue, newValue)) return;

        TaskHistory history = TaskHistory.builder()
                .task(task)
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .build();

        historyRepository.save(history);
    }

    /**
     * Task tarihçesini yeniden eskiye sıralı olarak getir.
     */
    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> getHistory(UUID taskId) {
        return historyRepository.findByTaskIdOrderByChangedAtDesc(taskId)
                .stream()
                .map(TaskHistoryResponse::from)
                .toList();
    }
}


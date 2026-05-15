package com.scrumtools.service;

import com.scrumtools.entity.Task;
import com.scrumtools.entity.WorkflowStatus;
import com.scrumtools.entity.WorkflowTransition;
import com.scrumtools.repository.WorkflowRepository;
import com.scrumtools.repository.WorkflowStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Workflow geçiş validasyonu ve aksiyon yürütme motoru.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;

    /**
     * Belirtilen geçişin geçerli olup olmadığını doğrular.
     * fromStatus null ise "herhangi bir yerden" geçiş anlamına gelir (global transition).
     */
    public boolean isTransitionAllowed(Task task, UUID targetStatusId, String userEmail) {
        // Workflow tanımlı değil veya geçiş bulunamıyorsa serbest geçiş izni (backward compat)
        Optional<WorkflowStatus> targetOpt = workflowStatusRepository.findById(targetStatusId);
        if (targetOpt.isEmpty()) {
            log.debug("Hedef status workflow'da bulunamadı — serbest geçiş izni verildi: {}", targetStatusId);
            return true;
        }

        WorkflowStatus targetStatus = targetOpt.get();
        UUID workflowId = targetStatus.getWorkflow().getId();

        List<WorkflowTransition> transitions = targetStatus.getWorkflow().getTransitions().stream()
                .filter(t -> t.getToStatus().getId().equals(targetStatusId))
                .toList();

        if (transitions.isEmpty()) {
            log.debug("Bu statüs için geçiş tanımlı değil — serbest geçiş izni: {}", targetStatus.getName());
            return true;
        }

        // Mevcut status'dan hedef status'a geçiş var mı?
        String currentStatus = task.getStatus();
        boolean hasValidTransition = transitions.stream().anyMatch(t ->
                t.getFromStatus() == null || // global transition
                t.getFromStatus().getName().equalsIgnoreCase(currentStatus) ||
                t.getFromStatus().getId().toString().equals(currentStatus)
        );

        if (!hasValidTransition) {
            log.warn("Geçersiz workflow geçişi: {} → {} (task={})",
                    currentStatus, targetStatus.getName(), task.getId());
            return false;
        }

        return true;
    }

    /**
     * Geçiş aksiyonlarını uygular (auto-assign, label ekleme vb.)
     */
    public void applyTransitionActions(Task task, WorkflowTransition transition, String performedBy) {
        if (transition.getActions() == null) return;

        var actions = transition.getActions();

        // autoAssignTo: "reporter" | "lead" | "<email>"
        if (actions.containsKey("autoAssignTo")) {
            String target = (String) actions.get("autoAssignTo");
            if ("reporter".equals(target) && task.getReporter() != null) {
                task.setAssignee(task.getReporter());
                log.debug("Auto-assign: {} → reporter ({})", task.getCustomId(), task.getReporter());
            } else if (target != null && target.contains("@")) {
                task.setAssignee(target);
                log.debug("Auto-assign: {} → {}", task.getCustomId(), target);
            }
        }

        // addLabel
        if (actions.containsKey("addLabel")) {
            String label = (String) actions.get("addLabel");
            if (label != null && !task.getLabels().contains(label)) {
                task.getLabels().add(label);
            }
        }

        log.info("Workflow aksiyonları uygulandı: task={}, transition={}", task.getCustomId(), transition.getName());
    }
}


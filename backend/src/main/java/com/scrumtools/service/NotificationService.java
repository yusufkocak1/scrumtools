package com.scrumtools.service;

import com.scrumtools.dto.NotificationResponse;
import com.scrumtools.entity.Notification;
import com.scrumtools.entity.enums.NotificationType;
import com.scrumtools.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Bildirim Oluşturma ───────────────────────────────────────────────────

    /**
     * Bildirim oluşturur ve WebSocket üzerinden push gönderir.
     * @Async: çağıran thread'i bloke etmez.
     * Transactional self-invocation sorunundan kaçınmak için save doğrudan burada yapılır.
     */
    @Async
    public void createAndPush(String recipientEmail,
                               NotificationType type,
                               String title,
                               String message,
                               String entityType,
                               String entityId,
                               Map<String, Object> data) {
        if (recipientEmail == null || recipientEmail.isBlank()) return;

        Notification notification = Notification.builder()
                .recipientEmail(recipientEmail)
                .type(type)
                .title(title)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .data(data)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse dto = NotificationResponse.from(saved);

        // WebSocket push — kullanıcıya özel topic
        try {
            messagingTemplate.convertAndSend(
                    "/topic/user/" + recipientEmail + "/notifications", dto);
        } catch (Exception ex) {
            log.warn("[Notification] WebSocket push başarısız ({}): {}", recipientEmail, ex.getMessage());
        }
    }

    // ─── Task Olayları İçin Yardımcı Metodlar ────────────────────────────────

    public void notifyTaskAssigned(String recipientEmail, String taskCustomId,
                                   String taskTitle, String teamId, String taskId) {
        Map<String, Object> data = new HashMap<>();
        data.put("teamId", teamId);
        data.put("taskId", taskId);
        data.put("customId", taskCustomId);

        createAndPush(
                recipientEmail,
                NotificationType.TASK_ASSIGNED,
                taskCustomId + " sana atandı",
                "\"" + taskTitle + "\" görevi sana atandı.",
                "task",
                taskId,
                data
        );
    }

    public void notifyTaskCommented(String recipientEmail, String actorEmail,
                                    String taskCustomId, String taskTitle,
                                    String teamId, String taskId) {
        Map<String, Object> data = new HashMap<>();
        data.put("teamId", teamId);
        data.put("taskId", taskId);
        data.put("customId", taskCustomId);

        createAndPush(
                recipientEmail,
                NotificationType.TASK_COMMENTED,
                taskCustomId + " görevine yorum yapıldı",
                actorEmail + " \"" + taskTitle + "\" görevine yorum ekledi.",
                "task",
                taskId,
                data
        );
    }

    public void notifyTaskStatusChanged(String recipientEmail, String actorEmail,
                                        String taskCustomId, String taskTitle,
                                        String oldStatus, String newStatus,
                                        String teamId, String taskId) {
        Map<String, Object> data = new HashMap<>();
        data.put("teamId", teamId);
        data.put("taskId", taskId);
        data.put("customId", taskCustomId);
        data.put("oldStatus", oldStatus);
        data.put("newStatus", newStatus);

        createAndPush(
                recipientEmail,
                NotificationType.TASK_STATUS_CHANGED,
                taskCustomId + " durumu değişti",
                actorEmail + " \"" + taskTitle + "\" durumunu " + oldStatus + " → " + newStatus + " olarak güncelledi.",
                "task",
                taskId,
                data
        );
    }

    public void notifyWatchedTaskUpdated(String recipientEmail, String actorEmail,
                                         String taskCustomId, String taskTitle,
                                         String field, String teamId, String taskId) {
        Map<String, Object> data = new HashMap<>();
        data.put("teamId", teamId);
        data.put("taskId", taskId);
        data.put("customId", taskCustomId);
        data.put("field", field);

        createAndPush(
                recipientEmail,
                NotificationType.WATCHED_TASK_UPDATED,
                "İzlediğin görev güncellendi: " + taskCustomId,
                actorEmail + " \"" + taskTitle + "\" görevinin " + field + " alanını güncelledi.",
                "task",
                taskId,
                data
        );
    }

    // ─── REST ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String email, int page, int size) {
        return notificationRepository
                .findByRecipientEmailOrderByCreatedAtDesc(email, PageRequest.of(page, size))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        return notificationRepository.countByRecipientEmailAndIsReadFalse(email);
    }

    @Transactional
    public void markRead(String email, UUID notificationId) {
        notificationRepository.markOneRead(notificationId, email);
    }

    @Transactional
    public void markAllRead(String email) {
        notificationRepository.markAllReadByEmail(email);
    }
}


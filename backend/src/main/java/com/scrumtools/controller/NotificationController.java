package com.scrumtools.controller;

import com.scrumtools.dto.ActivityEventResponse;
import com.scrumtools.dto.NotificationResponse;
import com.scrumtools.service.ActivityService;
import com.scrumtools.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ActivityService activityService;

    // ─── Notifications ────────────────────────────────────────────────────────

    /** Bildirim listesi (sayfalı) */
    @GetMapping("/api/notifications")
    public Page<NotificationResponse> getNotifications(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.getNotifications(user.getUsername(), page, size);
    }

    /** Okunmamış bildirim sayısı */
    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal UserDetails user) {
        return Map.of("count", notificationService.getUnreadCount(user.getUsername()));
    }

    /** Tek bildirimi okundu olarak işaretle */
    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails user,
                                         @PathVariable UUID id) {
        notificationService.markRead(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /** Tümünü okundu olarak işaretle */
    @PostMapping("/api/notifications/mark-all-read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllRead(user.getUsername());
        return ResponseEntity.noContent().build();
    }

    // ─── Activity Feed ────────────────────────────────────────────────────────

    /** Takım aktivite akışı */
    @GetMapping("/api/teams/{teamId}/activity")
    public Page<ActivityEventResponse> getTeamActivity(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return activityService.getTeamActivity(teamId, page, size);
    }
}


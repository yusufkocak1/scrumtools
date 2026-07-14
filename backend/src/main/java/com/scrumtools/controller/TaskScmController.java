package com.scrumtools.controller;

import com.scrumtools.dto.TaskScmResponse;
import com.scrumtools.service.scm.ScmTaskDevService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Task detayındaki Geliştirme (Dev) paneli — takım üyeliğiyle erişilir.
 * Faz 2'de branch oluşturma endpoint'i buraya eklenecek.
 */
@RestController
@RequestMapping("/api/teams/{teamId}/tasks/{taskId}/scm")
@RequiredArgsConstructor
public class TaskScmController {

    private final ScmTaskDevService taskDevService;

    @GetMapping
    public ResponseEntity<TaskScmResponse> getTaskScm(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(taskDevService.getTaskScm(teamId, taskId, currentUserEmail()));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

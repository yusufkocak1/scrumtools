package com.scrumtools.controller;

import com.scrumtools.dto.ScmBranchCreateRequest;
import com.scrumtools.dto.ScmBranchResponse;
import com.scrumtools.dto.ScmPullRequestCreateRequest;
import com.scrumtools.dto.ScmPullRequestResponse;
import com.scrumtools.dto.TaskScmResponse;
import com.scrumtools.service.scm.ScmTaskDevService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Task detayındaki Geliştirme (Dev) paneli — görüntüleme takım üyeliğiyle,
 * branch açma GIT_INTEGRATION + SCM_CREATE_BRANCH, pull request açma
 * GIT_INTEGRATION + SCM_CREATE_PULL_REQUEST ile (kontroller servis katmanında).
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

    /** Task'tan branch açar ve task'a bağlar. */
    @PostMapping("/branches")
    public ResponseEntity<ScmBranchResponse> createBranch(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody ScmBranchCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskDevService.createBranch(teamId, taskId, currentUserEmail(), request));
    }

    /** Task'a bağlı bir branch'ten pull request (GitLab'da merge request) açar. */
    @PostMapping("/pull-requests")
    public ResponseEntity<ScmPullRequestResponse> createPullRequest(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody ScmPullRequestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskDevService.createPullRequest(teamId, taskId, currentUserEmail(), request));
    }

    /** PR'ın sağlayıcıdaki güncel durumunu çeker (açık/merged/kapalı). */
    @PostMapping("/pull-requests/{pullRequestId}/refresh")
    public ResponseEntity<ScmPullRequestResponse> refreshPullRequest(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @PathVariable UUID pullRequestId
    ) {
        return ResponseEntity.ok(
                taskDevService.refreshPullRequest(teamId, taskId, pullRequestId, currentUserEmail()));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

package com.scrumtools.dto;

import java.util.UUID;

/**
 * Task'a bağlı bir branch'ten pull request açma isteği.
 * targetBranch boşsa reponun default branch'i, title boşsa "TASKKEY Görev başlığı" kullanılır.
 */
public record ScmPullRequestCreateRequest(
        UUID branchId,
        String targetBranch,
        String title,
        String description,
        boolean draft
) {}

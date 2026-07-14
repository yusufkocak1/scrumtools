package com.scrumtools.dto;

import java.util.UUID;

/** Task'tan branch açma isteği. sourceRef boşsa reponun default branch'i kullanılır. */
public record ScmBranchCreateRequest(
        UUID repositoryId,
        String branchName,
        String sourceRef
) {}

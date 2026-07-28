package com.scrumtools.service.scm.client;

import com.scrumtools.entity.enums.ScmPullRequestState;

import java.time.LocalDateTime;

/** Sağlayıcıdan dönen pull/merge request bilgisi (durum normalize edilmiş). */
public record ScmPullRequestInfo(
        String externalId,
        String title,
        String sourceBranch,
        String targetBranch,
        ScmPullRequestState state,
        String webUrl,
        boolean draft,
        LocalDateTime mergedAt
) {}

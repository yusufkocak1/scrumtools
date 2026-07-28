package com.scrumtools.entity.enums;

/**
 * Task'a bağlı pull/merge request'in durumu.
 * Sağlayıcı durumları normalize edilir: GitHub merged_at dolu → MERGED,
 * state=closed → CLOSED; GitLab opened → OPEN, merged → MERGED, closed/locked → CLOSED.
 */
public enum ScmPullRequestState {
    OPEN,
    MERGED,
    CLOSED
}

package com.scrumtools.entity.enums;

/**
 * Desteklenen SCM (kaynak kod yönetimi) sağlayıcıları.
 * Self-hosted kurulumlar (GitHub Enterprise, GitLab self-managed, Gitea/Forgejo)
 * ScmConnection.baseUrl ile aynı enum değerleri üzerinden desteklenir.
 */
public enum ScmProvider {
    GITHUB,
    GITLAB,
    BITBUCKET,
    GITEA
}

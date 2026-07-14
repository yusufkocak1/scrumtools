package com.scrumtools.dto;

import java.util.List;
import java.util.UUID;

/**
 * Task detayındaki Geliştirme (Dev) paneli verisi.
 * projectLinked=false → takım projeye bağlı değil; repos boş → repo eşlenmemiş
 * (canManageRepos=true ise UI yöneticiye eşleme linki gösterir).
 */
public record TaskScmResponse(
        boolean featureEnabled,
        boolean projectLinked,
        UUID projectId,
        boolean canCreateBranch,
        boolean canManageRepos,
        boolean hasUserAccount,
        List<ScmRepositoryResponse> repos,
        List<ScmBranchResponse> branches,
        List<ScmCommitResponse> commits
) {}

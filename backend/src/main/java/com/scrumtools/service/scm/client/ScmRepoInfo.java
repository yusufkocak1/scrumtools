package com.scrumtools.service.scm.client;

/** Sağlayıcıdan canlı çekilen repo bilgisi (keşif/eşleme ekranı için). */
public record ScmRepoInfo(
        String externalId,
        String name,
        String fullName,
        String defaultBranch,
        String webUrl,
        boolean privateRepo
) {}

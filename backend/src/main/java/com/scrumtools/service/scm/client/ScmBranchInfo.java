package com.scrumtools.service.scm.client;

/** Sağlayıcıdan canlı çekilen branch bilgisi. */
public record ScmBranchInfo(
        String name,
        String sha,
        String webUrl
) {}

package com.scrumtools.dto;

import com.scrumtools.service.scm.client.ScmRepoInfo;

/** Sağlayıcıdan canlı çekilen repo — eşleme ekranında listelenir. */
public record ScmRemoteRepoResponse(
        String externalId,
        String name,
        String fullName,
        String defaultBranch,
        String webUrl,
        boolean privateRepo,
        boolean mapped
) {
    public static ScmRemoteRepoResponse from(ScmRepoInfo info, boolean mapped) {
        return new ScmRemoteRepoResponse(
                info.externalId(), info.name(), info.fullName(),
                info.defaultBranch(), info.webUrl(), info.privateRepo(), mapped);
    }
}

package com.scrumtools.service.scm.client;

import java.time.LocalDateTime;

/** Sağlayıcıdan çekilen commit bilgisi (poller ve canlı listeleme için). */
public record ScmCommitInfo(
        String sha,
        String message,
        String authorName,
        String authorEmail,
        LocalDateTime authoredAt,
        String webUrl
) {}

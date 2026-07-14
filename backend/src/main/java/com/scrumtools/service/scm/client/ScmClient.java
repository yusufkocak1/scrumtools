package com.scrumtools.service.scm.client;

import com.scrumtools.entity.ScmRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sağlayıcı soyutlaması — GitHub / GitLab / Bitbucket / Gitea implementasyonları
 * bu dar arayüzü doldurur. Instance'lar ScmClientFactory tarafından istek başına
 * üretilir (token bağlantıdan çözülür), Spring bean'i değildir.
 */
public interface ScmClient {

    /** Token'ı doğrular; başarılıysa sağlayıcıdaki kullanıcı bilgisini döner. */
    ScmUserInfo validateToken();

    /** Token'ın erişebildiği repoları listeler (page 1 tabanlı). */
    List<ScmRepoInfo> listRepositories(String search, int page);

    /** Tek repo detayı — eşleme sırasında meta bilgileri çekmek için. */
    ScmRepoInfo getRepository(String externalIdOrFullName);

    List<ScmBranchInfo> listBranches(ScmRepository repo, String search);

    ScmBranchInfo createBranch(ScmRepository repo, String branchName, String sourceRef);

    List<ScmCommitInfo> listCommits(ScmRepository repo, String ref, LocalDateTime since);

    WebhookRegistration registerWebhook(ScmRepository repo, String callbackUrl, String secret);

    void removeWebhook(ScmRepository repo, String externalWebhookId);
}

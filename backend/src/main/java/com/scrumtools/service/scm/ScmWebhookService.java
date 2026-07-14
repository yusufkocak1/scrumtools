package com.scrumtools.service.scm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumtools.entity.ScmBranch;
import com.scrumtools.entity.ScmCommit;
import com.scrumtools.entity.ScmConnection;
import com.scrumtools.entity.ScmRepository;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.enums.ScmBranchStatus;
import com.scrumtools.entity.enums.ScmProvider;
import com.scrumtools.repository.ScmBranchRepository;
import com.scrumtools.repository.ScmCommitRepository;
import com.scrumtools.repository.ScmConnectionRepository;
import com.scrumtools.repository.ScmRepositoryRepository;
import com.scrumtools.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SCM push webhook'larını işler. Güvenlik felsefesi iyzico webhook'uyla aynı:
 * imza doğrulanamazsa SESSİZCE yutulur (saldırgana bilgi sızdırılmaz, sağlayıcının
 * retry fırtınası tetiklenmez); idempotency (repository_id, sha) unique kısıtı +
 * ön kontrolle sağlanır. Sahte payload en fazla boş bir sorgu tetikler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScmWebhookService {

    /** Tek push'ta işlenecek maksimum commit (devasa push'lara karşı koruma). */
    private static final int MAX_COMMITS_PER_PUSH = 100;

    private final ScmConnectionRepository connectionRepository;
    private final ScmRepositoryRepository scmRepositoryRepository;
    private final ScmCommitRepository scmCommitRepository;
    private final ScmBranchRepository scmBranchRepository;
    private final ScmCommitLinker commitLinker;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void handle(String providerPath, UUID connectionId, String rawBody, HttpHeaders headers) {
        ScmProvider provider = parseProvider(providerPath);
        if (provider == null) return;

        Optional<ScmConnection> connectionOpt = connectionRepository.findById(connectionId);
        if (connectionOpt.isEmpty() || connectionOpt.get().getProvider() != provider) {
            log.warn("SCM webhook: bağlantı bulunamadı veya provider uyuşmuyor (id={})", connectionId);
            return;
        }
        ScmConnection connection = connectionOpt.get();

        if (!verifySignature(provider, connection.getWebhookSecret(), rawBody, headers)) {
            log.warn("SCM webhook: imza doğrulanamadı (connection={})", connectionId);
            return;
        }
        if (!isPushEvent(provider, headers)) {
            return; // ping, star vb. eventler sessizce yutulur
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("SCM webhook: payload parse edilemedi: {}", e.getMessage());
            return;
        }

        String repoExternalId = extractRepoExternalId(provider, payload);
        if (repoExternalId == null) return;

        // Ek savunma: payload'daki repo kimliği DB'dekiyle eşleşmeli
        Optional<ScmRepository> repoOpt =
                scmRepositoryRepository.findByConnectionIdAndExternalId(connectionId, repoExternalId);
        if (repoOpt.isEmpty()) {
            log.debug("SCM webhook: eşlenmemiş repo, atlanıyor ({})", repoExternalId);
            return;
        }
        ScmRepository repo = repoOpt.get();

        String ref = str(payload.get("ref"));
        String branchName = branchFromRef(ref);
        List<Map<String, Object>> commits = asList(payload.get("commits"));
        boolean suspended = repo.getConnection().getOrganization().isSuspendedOrg();

        // Branch keşfi: adında task anahtarı geçen branch'ler push'ta task'a bağlanır
        if (ref != null && ref.startsWith("refs/heads/")) {
            processBranch(repo, branchName, str(payload.get("after")),
                    isBranchDeleted(payload), pusherIdentity(payload), suspended);
        }

        int linked = 0;
        for (Map<String, Object> commit : commits.stream().limit(MAX_COMMITS_PER_PUSH).toList()) {
            if (processCommit(repo, commit, branchName, suspended)) linked++;
        }

        repo.setLastSyncedAt(LocalDateTime.now());
        scmRepositoryRepository.save(repo);

        if (linked > 0) {
            log.info("SCM webhook: {} commit task'lara bağlandı (repo={})", linked, repo.getFullName());
        }
    }

    /**
     * Push edilen branch'i işler: adında task anahtarı geçiyorsa ilgili task'lara
     * bağlanır (upsert). Silinen branch DELETED işaretlenir, yeniden push'lanan
     * tekrar ACTIVE olur — kayıt silinmez, görünürlük kaybolmaz.
     */
    private void processBranch(ScmRepository repo, String branchName, String afterSha,
                               boolean deleted, String pusher, boolean suspendedOrg) {
        List<Task> tasks = commitLinker.resolveTasks(repo, branchName);
        if (tasks.isEmpty()) return;

        for (Task task : tasks) {
            ScmBranch branch = scmBranchRepository
                    .findByRepositoryIdAndTaskIdAndName(repo.getId(), task.getId(), branchName)
                    .orElse(null);

            if (deleted) {
                if (branch != null && branch.getStatus() != ScmBranchStatus.DELETED) {
                    branch.setStatus(ScmBranchStatus.DELETED);
                    scmBranchRepository.save(branch);
                }
                continue;
            }

            if (branch == null) {
                branch = ScmBranch.builder()
                        .repository(repo)
                        .task(task)
                        .name(branchName)
                        .webUrl(branchWebUrl(repo, branchName))
                        .createdViaApp(false)
                        .status(ScmBranchStatus.ACTIVE)
                        .lastCommitSha(afterSha)
                        .build();
                scmBranchRepository.save(branch);
                if (!suspendedOrg) {
                    auditService.recordChange(task, "branch", null, branchName, pusher);
                }
                log.info("SCM webhook: branch keşfedildi ve bağlandı: {} → {}",
                        branchName, task.getCustomId());
            } else {
                branch.setStatus(ScmBranchStatus.ACTIVE);
                if (afterSha != null) branch.setLastCommitSha(afterSha);
                scmBranchRepository.save(branch);
            }
        }
    }

    /** Tek commit'i işler; bir task'a bağlandıysa true döner. */
    private boolean processCommit(ScmRepository repo, Map<String, Object> commit,
                                  String branchName, boolean suspendedOrg) {
        String sha = str(commit.get("id"));
        String message = str(commit.get("message"));
        if (sha == null || message == null) return false;
        if (scmCommitRepository.existsByRepositoryIdAndSha(repo.getId(), sha)) return false; // idempotent

        // Anahtar commit mesajında YA DA branch adında geçebilir (feature/DEV-19'a
        // atılan commit'ler mesajda anahtar olmasa da task'a bağlanır)
        List<Task> tasks = commitLinker.resolveTasks(repo,
                branchName == null ? message : message + "\n" + branchName);
        if (tasks.isEmpty()) return false; // eşleşmeyen commit kaydedilmez (tablo şişmesi önlenir)

        Map<String, Object> author = asMap(commit.get("author"));
        String authorName = str(author.get("name"));
        String authorEmail = str(author.get("email"));

        ScmCommit scmCommit = ScmCommit.builder()
                .repository(repo)
                .sha(sha)
                .shortMessage(shortMessage(message))
                .authorName(authorName)
                .authorEmail(authorEmail)
                .authoredAt(ScmDates.parse(commit.get("timestamp")))
                .webUrl(str(commit.get("url")))
                .branchHint(branchName)
                .tasks(new HashSet<>(tasks))
                .build();
        scmCommitRepository.save(scmCommit);

        if (!suspendedOrg) {
            // Askıya alınmış org'da veri birikimi sürer ama tarihçe/aktivite üretilmez
            String changedBy = authorEmail != null ? authorEmail : authorName;
            for (Task task : tasks) {
                auditService.recordChange(task, "commit", null,
                        shortSha(sha) + " — " + shortMessage(message), changedBy);
            }
        }
        return true;
    }

    // ─── İmza doğrulama ───────────────────────────────────────────────────────

    /**
     * Sağlayıcıya göre webhook imzasını doğrular (constant-time karşılaştırma).
     * GitHub/Bitbucket: X-Hub-Signature-256 (sha256=hex HMAC), Gitea: X-Gitea-Signature,
     * GitLab: X-Gitlab-Token (düz secret karşılaştırması).
     */
    public static boolean verifySignature(ScmProvider provider, String secret,
                                          String rawBody, HttpHeaders headers) {
        if (secret == null || secret.isBlank()) return false;
        return switch (provider) {
            case GITHUB, BITBUCKET -> {
                String header = headers.getFirst("X-Hub-Signature-256");
                yield header != null && constantTimeEquals(
                        header, "sha256=" + hmacSha256Hex(secret, rawBody));
            }
            case GITEA -> {
                String header = headers.getFirst("X-Gitea-Signature");
                yield header != null && constantTimeEquals(header, hmacSha256Hex(secret, rawBody));
            }
            case GITLAB -> {
                String header = headers.getFirst("X-Gitlab-Token");
                yield header != null && constantTimeEquals(header, secret);
            }
        };
    }

    private static String hmacSha256Hex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC hesaplanamadı", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Payload ayrıştırma ───────────────────────────────────────────────────

    private ScmProvider parseProvider(String providerPath) {
        try {
            return ScmProvider.valueOf(providerPath.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isPushEvent(ScmProvider provider, HttpHeaders headers) {
        return switch (provider) {
            case GITHUB -> "push".equals(headers.getFirst("X-GitHub-Event"));
            case GITLAB -> "Push Hook".equals(headers.getFirst("X-Gitlab-Event"));
            case GITEA -> "push".equals(headers.getFirst("X-Gitea-Event"));
            case BITBUCKET -> {
                String key = headers.getFirst("X-Event-Key");
                yield key != null && key.startsWith("repo:push");
            }
        };
    }

    private String extractRepoExternalId(ScmProvider provider, Map<String, Object> payload) {
        return switch (provider) {
            // GitHub/Gitea: externalId = "owner/name"
            case GITHUB, GITEA, BITBUCKET -> str(asMap(payload.get("repository")).get("full_name"));
            // GitLab: externalId = numeric project id
            case GITLAB -> str(payload.get("project_id")) != null
                    ? str(payload.get("project_id"))
                    : str(asMap(payload.get("project")).get("id"));
        };
    }

    private String branchFromRef(String ref) {
        if (ref == null) return null;
        return ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;
    }

    /** GitHub/Gitea: deleted=true; GitLab: after tamamı sıfır SHA. */
    private boolean isBranchDeleted(Map<String, Object> payload) {
        if (Boolean.TRUE.equals(payload.get("deleted"))) return true;
        String after = str(payload.get("after"));
        return after != null && !after.isEmpty() && after.chars().allMatch(c -> c == '0');
    }

    /** Push'u yapan kişi (audit için) — sağlayıcıya göre farklı alanlarda gelir. */
    private String pusherIdentity(Map<String, Object> payload) {
        Map<String, Object> pusher = asMap(payload.get("pusher"));
        String identity = str(pusher.get("email"));                       // GitHub/Gitea
        if (identity == null) identity = str(pusher.get("name"));         // GitHub
        if (identity == null) identity = str(pusher.get("login"));        // Gitea
        if (identity == null) identity = str(payload.get("user_email"));  // GitLab
        if (identity == null) identity = str(payload.get("user_username"));
        return identity;
    }

    /** Sağlayıcıya göre branch web adresi (webhook'ta hazır gelmez, türetilir). */
    private String branchWebUrl(ScmRepository repo, String branchName) {
        if (repo.getWebUrl() == null) return null;
        String path = switch (repo.getConnection().getProvider()) {
            case GITLAB -> "/-/tree/";
            case GITEA -> "/src/branch/";
            case BITBUCKET -> "/branch/";
            case GITHUB -> "/tree/";
        };
        return repo.getWebUrl() + path + branchName;
    }

    private String shortMessage(String message) {
        String firstLine = message.lines().findFirst().orElse("").trim();
        return firstLine.length() > 500 ? firstLine.substring(0, 500) : firstLine;
    }

    private String shortSha(String sha) {
        return sha.length() > 7 ? sha.substring(0, 7) : sha;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        return o instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }
}

package com.scrumtools.service.scm.client;

import com.scrumtools.entity.ScmRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.scrumtools.service.scm.client.ScmClientSupport.*;

/**
 * GitHub REST API v3 istemcisi (cloud + GitHub Enterprise).
 * Auth: Authorization: Bearer <PAT> (fine-grained veya classic, scope: repo).
 * Gitea/Forgejo API'si büyük oranda uyumlu olduğundan GiteaClient bunu genişletebilir.
 */
public class GitHubClient implements ScmClient {

    private static final String PROVIDER = "GitHub";
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public GitHubClient(String baseUrl, String token) {
        String apiBase = (baseUrl == null || baseUrl.isBlank())
                ? "https://api.github.com"
                : baseUrl.replaceAll("/+$", "") + "/api/v3";
        this.restClient = RestClient.builder()
                .baseUrl(apiBase)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Override
    public ScmUserInfo validateToken() {
        try {
            Map<String, Object> user = restClient.get().uri("/user").retrieve().body(MAP_TYPE);
            return new ScmUserInfo(str(user.get("login")), str(user.get("email")));
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "token doğrulama");
        }
    }

    @Override
    public List<ScmRepoInfo> listRepositories(String search, int page) {
        try {
            List<Map<String, Object>> repos = restClient.get()
                    .uri("/user/repos?per_page=100&page={page}&sort=updated&affiliation=owner,collaborator,organization_member",
                            Math.max(page, 1))
                    .retrieve().body(LIST_TYPE);
            String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            return repos.stream()
                    .filter(r -> q.isEmpty() || str(r.get("full_name")).toLowerCase(Locale.ROOT).contains(q))
                    .map(this::toRepoInfo)
                    .toList();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "repo listeleme");
        }
    }

    @Override
    public ScmRepoInfo getRepository(String fullName) {
        try {
            Map<String, Object> repo = restClient.get().uri("/repos/" + fullName).retrieve().body(MAP_TYPE);
            return toRepoInfo(repo);
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "repo sorgulama");
        }
    }

    @Override
    public List<ScmBranchInfo> listBranches(ScmRepository repo, String search) {
        try {
            List<Map<String, Object>> branches = restClient.get()
                    .uri("/repos/" + repo.getExternalId() + "/branches?per_page=100")
                    .retrieve().body(LIST_TYPE);
            String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            return branches.stream()
                    .filter(b -> q.isEmpty() || str(b.get("name")).toLowerCase(Locale.ROOT).contains(q))
                    .map(b -> new ScmBranchInfo(
                            str(b.get("name")),
                            str(asMap(b.get("commit")).get("sha")),
                            branchUrl(repo, str(b.get("name")))))
                    .toList();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "branch listeleme");
        }
    }

    @Override
    public ScmBranchInfo createBranch(ScmRepository repo, String branchName, String sourceRef) {
        try {
            // Önce base ref'in SHA'sı alınır, sonra yeni ref oluşturulur
            Map<String, Object> baseRef = restClient.get()
                    .uri("/repos/" + repo.getExternalId() + "/git/ref/heads/" + sourceRef)
                    .retrieve().body(MAP_TYPE);
            String baseSha = str(asMap(baseRef.get("object")).get("sha"));

            restClient.post()
                    .uri("/repos/" + repo.getExternalId() + "/git/refs")
                    .body(Map.of("ref", "refs/heads/" + branchName, "sha", baseSha))
                    .retrieve().body(MAP_TYPE);
            return new ScmBranchInfo(branchName, baseSha, branchUrl(repo, branchName));
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "branch oluşturma");
        }
    }

    @Override
    public List<ScmCommitInfo> listCommits(ScmRepository repo, String ref, LocalDateTime since) {
        try {
            StringBuilder uri = new StringBuilder("/repos/" + repo.getExternalId() + "/commits?per_page=50");
            if (ref != null && !ref.isBlank()) uri.append("&sha=").append(ref);
            if (since != null) uri.append("&since=").append(toIsoUtc(since));

            List<Map<String, Object>> commits = restClient.get().uri(uri.toString()).retrieve().body(LIST_TYPE);
            return commits.stream().map(c -> {
                Map<String, Object> commit = asMap(c.get("commit"));
                Map<String, Object> author = asMap(commit.get("author"));
                return new ScmCommitInfo(
                        str(c.get("sha")),
                        str(commit.get("message")),
                        str(author.get("name")),
                        str(author.get("email")),
                        parseDate(author.get("date")),
                        str(c.get("html_url")));
            }).toList();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "commit listeleme");
        }
    }

    @Override
    public WebhookRegistration registerWebhook(ScmRepository repo, String callbackUrl, String secret) {
        try {
            Map<String, Object> hook = restClient.post()
                    .uri("/repos/" + repo.getExternalId() + "/hooks")
                    .body(Map.of(
                            "name", "web",
                            "active", true,
                            "events", List.of("push"),
                            "config", Map.of(
                                    "url", callbackUrl,
                                    "content_type", "json",
                                    "secret", secret,
                                    "insecure_ssl", "0")))
                    .retrieve().body(MAP_TYPE);
            return new WebhookRegistration(str(hook.get("id")));
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "webhook kaydı");
        }
    }

    @Override
    public void removeWebhook(ScmRepository repo, String externalWebhookId) {
        try {
            restClient.delete()
                    .uri("/repos/" + repo.getExternalId() + "/hooks/" + externalWebhookId)
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "webhook silme");
        }
    }

    private ScmRepoInfo toRepoInfo(Map<String, Object> repo) {
        return new ScmRepoInfo(
                str(repo.get("full_name")),
                str(repo.get("name")),
                str(repo.get("full_name")),
                str(repo.get("default_branch")),
                str(repo.get("html_url")),
                Boolean.TRUE.equals(repo.get("private")));
    }

    private String branchUrl(ScmRepository repo, String branchName) {
        return repo.getWebUrl() != null ? repo.getWebUrl() + "/tree/" + branchName : null;
    }
}

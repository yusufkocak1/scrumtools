package com.scrumtools.service.scm.client;

import com.scrumtools.entity.ScmRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.scrumtools.service.scm.client.ScmClientSupport.*;

/**
 * GitLab REST API v4 istemcisi (gitlab.com + self-managed).
 * Auth: PRIVATE-TOKEN header (PAT, scope: api).
 * externalId = GitLab'ın numeric project id'si (String olarak saklanır).
 */
public class GitLabClient implements ScmClient {

    private static final String PROVIDER = "GitLab";
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public GitLabClient(String baseUrl, String token) {
        String apiBase = (baseUrl == null || baseUrl.isBlank())
                ? "https://gitlab.com/api/v4"
                : baseUrl.replaceAll("/+$", "") + "/api/v4";
        this.restClient = RestClient.builder()
                .baseUrl(apiBase)
                .defaultHeader("PRIVATE-TOKEN", token)
                .build();
    }

    @Override
    public ScmUserInfo validateToken() {
        try {
            Map<String, Object> user = restClient.get().uri("/user").retrieve().body(MAP_TYPE);
            return new ScmUserInfo(str(user.get("username")), str(user.get("email")));
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "token doğrulama");
        }
    }

    @Override
    public List<ScmRepoInfo> listRepositories(String search, int page) {
        try {
            String uri = "/projects?membership=true&per_page=100&page=" + Math.max(page, 1)
                    + "&order_by=last_activity_at";
            if (search != null && !search.isBlank()) {
                uri += "&search=" + UriUtils.encodeQueryParam(search.trim(), StandardCharsets.UTF_8);
            }
            List<Map<String, Object>> projects = restClient.get().uri(uri).retrieve().body(LIST_TYPE);
            return projects.stream().map(this::toRepoInfo).toList();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "repo listeleme");
        }
    }

    @Override
    public ScmRepoInfo getRepository(String externalId) {
        try {
            Map<String, Object> project = restClient.get()
                    .uri("/projects/" + encodePath(externalId))
                    .retrieve().body(MAP_TYPE);
            return toRepoInfo(project);
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "repo sorgulama");
        }
    }

    @Override
    public List<ScmBranchInfo> listBranches(ScmRepository repo, String search) {
        try {
            String uri = "/projects/" + repo.getExternalId() + "/repository/branches?per_page=100";
            if (search != null && !search.isBlank()) {
                uri += "&search=" + UriUtils.encodeQueryParam(search.trim(), StandardCharsets.UTF_8);
            }
            List<Map<String, Object>> branches = restClient.get().uri(uri).retrieve().body(LIST_TYPE);
            return branches.stream()
                    .map(b -> new ScmBranchInfo(
                            str(b.get("name")),
                            str(asMap(b.get("commit")).get("id")),
                            str(b.get("web_url"))))
                    .toList();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "branch listeleme");
        }
    }

    @Override
    public ScmBranchInfo createBranch(ScmRepository repo, String branchName, String sourceRef) {
        try {
            Map<String, Object> branch = restClient.post()
                    .uri("/projects/" + repo.getExternalId() + "/repository/branches?branch={b}&ref={r}",
                            branchName, sourceRef)
                    .retrieve().body(MAP_TYPE);
            return new ScmBranchInfo(
                    str(branch.get("name")),
                    str(asMap(branch.get("commit")).get("id")),
                    str(branch.get("web_url")));
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "branch oluşturma");
        }
    }

    @Override
    public List<ScmCommitInfo> listCommits(ScmRepository repo, String ref, LocalDateTime since) {
        try {
            StringBuilder uri = new StringBuilder("/projects/" + repo.getExternalId()
                    + "/repository/commits?per_page=50");
            if (ref != null && !ref.isBlank()) {
                uri.append("&ref_name=").append(UriUtils.encodeQueryParam(ref, StandardCharsets.UTF_8));
            }
            if (since != null) uri.append("&since=").append(toIsoUtc(since));

            List<Map<String, Object>> commits = restClient.get().uri(uri.toString()).retrieve().body(LIST_TYPE);
            return commits.stream().map(c -> new ScmCommitInfo(
                    str(c.get("id")),
                    str(c.get("message")),
                    str(c.get("author_name")),
                    str(c.get("author_email")),
                    parseDate(c.get("created_at")),
                    str(c.get("web_url")))).toList();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "commit listeleme");
        }
    }

    @Override
    public WebhookRegistration registerWebhook(ScmRepository repo, String callbackUrl, String secret) {
        try {
            Map<String, Object> hook = restClient.post()
                    .uri("/projects/" + repo.getExternalId() + "/hooks")
                    .body(Map.of(
                            "url", callbackUrl,
                            "push_events", true,
                            "token", secret,
                            "enable_ssl_verification", true))
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
                    .uri("/projects/" + repo.getExternalId() + "/hooks/" + externalWebhookId)
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw wrap(e, PROVIDER, "webhook silme");
        }
    }

    private ScmRepoInfo toRepoInfo(Map<String, Object> project) {
        return new ScmRepoInfo(
                str(project.get("id")),
                str(project.get("path")),
                str(project.get("path_with_namespace")),
                str(project.get("default_branch")),
                str(project.get("web_url")),
                !"public".equals(str(project.get("visibility"))));
    }

    /** Numeric id ise olduğu gibi; "grup/proje" path'i ise URL-encode edilir. */
    private String encodePath(String externalId) {
        return externalId.chars().allMatch(Character::isDigit)
                ? externalId
                : UriUtils.encode(externalId, StandardCharsets.UTF_8);
    }
}

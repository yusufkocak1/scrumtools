package com.scrumtools.service.scm;

import com.scrumtools.dto.ScmConnectionRequest;
import com.scrumtools.dto.ScmConnectionResponse;
import com.scrumtools.dto.ScmConnectionTestResponse;
import com.scrumtools.dto.ScmRemoteRepoResponse;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.ScmConnection;
import com.scrumtools.entity.ScmRepository;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.ScmConnectionStatus;
import com.scrumtools.exception.PlanLimitExceededException;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.ScmBranchRepository;
import com.scrumtools.repository.ScmCommitRepository;
import com.scrumtools.repository.ScmConnectionRepository;
import com.scrumtools.repository.ScmRepositoryRepository;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.scm.client.ScmApiException;
import com.scrumtools.service.scm.client.ScmClient;
import com.scrumtools.service.scm.client.ScmClientFactory;
import com.scrumtools.service.scm.client.ScmUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Org seviyesi SCM bağlantılarının yönetimi (CRUD + test + canlı repo keşfi).
 * Tüm işlemler ORG_OWNER/ORG_ADMIN yetkisi ve GIT_INTEGRATION entitlement'ı ister.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScmConnectionService {

    /** Plan koduna göre bağlantı sayısı limiti; listede olmayan plan = sınırsız. */
    private static final Map<String, Integer> CONNECTION_LIMITS = Map.of("PRO", 1);

    private final ScmConnectionRepository connectionRepository;
    private final ScmRepositoryRepository scmRepositoryRepository;
    private final ScmBranchRepository scmBranchRepository;
    private final ScmCommitRepository scmCommitRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EntitlementService entitlementService;
    private final ScmTokenCrypto tokenCrypto;
    private final ScmUrlValidator urlValidator;
    private final ScmClientFactory clientFactory;

    @Transactional(readOnly = true)
    public List<ScmConnectionResponse> list(UUID orgId, String email) {
        checkAdminAccess(orgId, email);
        return connectionRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId).stream()
                .map(c -> ScmConnectionResponse.from(c, scmRepositoryRepository.countByConnectionId(c.getId())))
                .toList();
    }

    @Transactional
    public ScmConnectionResponse create(UUID orgId, String email, ScmConnectionRequest request) {
        checkAdminAccess(orgId, email);
        requireCryptoConfigured();
        entitlementService.assertFeature(orgId, PlanFeature.GIT_INTEGRATION);
        checkConnectionLimit(orgId);

        if (request.provider() == null) throw new IllegalArgumentException("Sağlayıcı seçilmeli.");
        if (isBlank(request.name())) throw new IllegalArgumentException("Bağlantı adı zorunlu.");
        if (isBlank(request.token())) throw new IllegalArgumentException("Erişim token'ı (PAT) zorunlu.");
        urlValidator.validate(request.baseUrl());

        // Token kaydetmeden önce sağlayıcıda doğrulanır — geçersizse hata döner
        ScmClient client = clientFactory.forRawToken(request.provider(),
                normalizeBaseUrl(request.baseUrl()), request.token().trim());
        ScmUserInfo userInfo = client.validateToken();

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));

        ScmConnection connection = ScmConnection.builder()
                .organization(org)
                .provider(request.provider())
                .name(request.name().trim())
                .baseUrl(normalizeBaseUrl(request.baseUrl()))
                .encryptedToken(tokenCrypto.encrypt(request.token().trim()))
                .tokenHint(tokenCrypto.hint(request.token().trim()))
                .username(isBlank(request.username()) ? userInfo.username() : request.username().trim())
                .webhookSecret(generateWebhookSecret())
                .status(ScmConnectionStatus.ACTIVE)
                .createdBy(email)
                .lastValidatedAt(LocalDateTime.now())
                .build();

        connection = connectionRepository.save(connection);
        log.info("SCM bağlantısı eklendi: org={} provider={} name='{}'",
                org.getSlug(), request.provider(), connection.getName());
        return ScmConnectionResponse.from(connection, 0);
    }

    @Transactional
    public ScmConnectionResponse update(UUID orgId, String email, UUID connectionId, ScmConnectionRequest request) {
        checkAdminAccess(orgId, email);
        ScmConnection connection = getConnection(orgId, connectionId);

        if (!isBlank(request.name())) connection.setName(request.name().trim());
        if (request.username() != null) {
            connection.setUsername(isBlank(request.username()) ? null : request.username().trim());
        }

        boolean tokenChanged = !isBlank(request.token());
        if (tokenChanged) {
            requireCryptoConfigured();
            // Yeni token kaydedilmeden önce doğrulanır
            ScmClient client = clientFactory.forRawToken(connection.getProvider(),
                    connection.getBaseUrl(), request.token().trim());
            client.validateToken();
            connection.setEncryptedToken(tokenCrypto.encrypt(request.token().trim()));
            connection.setTokenHint(tokenCrypto.hint(request.token().trim()));
            connection.setStatus(ScmConnectionStatus.ACTIVE);
            connection.setLastValidatedAt(LocalDateTime.now());
        }

        connection = connectionRepository.save(connection);
        return ScmConnectionResponse.from(connection,
                scmRepositoryRepository.countByConnectionId(connection.getId()));
    }

    /**
     * Bağlantıyı ve ona bağlı tüm eşlemeleri kaldırır. Sağlayıcıdaki webhook'lar
     * best-effort silinir (token geçersizse eşleme silme yine tamamlanır).
     */
    @Transactional
    public void delete(UUID orgId, String email, UUID connectionId) {
        checkAdminAccess(orgId, email);
        ScmConnection connection = getConnection(orgId, connectionId);

        List<ScmRepository> repos = scmRepositoryRepository.findByConnectionId(connectionId);
        for (ScmRepository repo : repos) {
            removeWebhookQuietly(connection, repo);
            scmBranchRepository.deleteByRepositoryId(repo.getId());
            scmCommitRepository.deleteAll(scmCommitRepository.findByRepositoryId(repo.getId()));
        }
        scmRepositoryRepository.deleteAll(repos);
        connectionRepository.delete(connection);
        log.info("SCM bağlantısı silindi: {} ({} repo eşlemesiyle birlikte)", connection.getName(), repos.size());
    }

    /** Token'ı canlı test eder; sonuç ne olursa olsun 200 döner (ok flag'i ile). */
    @Transactional
    public ScmConnectionTestResponse test(UUID orgId, String email, UUID connectionId) {
        checkAdminAccess(orgId, email);
        ScmConnection connection = getConnection(orgId, connectionId);
        try {
            ScmUserInfo info = clientFactory.forConnection(connection).validateToken();
            connection.setStatus(ScmConnectionStatus.ACTIVE);
            connection.setLastValidatedAt(LocalDateTime.now());
            connectionRepository.save(connection);
            return new ScmConnectionTestResponse(true,
                    "Bağlantı başarılı (" + info.username() + ").", info.username());
        } catch (ScmApiException e) {
            if (e.isAuthFailure()) {
                connection.setStatus(ScmConnectionStatus.TOKEN_INVALID);
                connectionRepository.save(connection);
            }
            return new ScmConnectionTestResponse(false, e.getMessage(), null);
        }
    }

    /** Sağlayıcıdan CANLI repo listesi — eşleme ekranı için (eşlenmişler işaretlenir). */
    @Transactional(readOnly = true)
    public List<ScmRemoteRepoResponse> listRemoteRepos(UUID orgId, String email, UUID connectionId,
                                                       String search, int page) {
        checkAdminAccess(orgId, email);
        ScmConnection connection = getConnection(orgId, connectionId);

        Set<String> mappedExternalIds = scmRepositoryRepository.findByConnectionId(connectionId).stream()
                .map(ScmRepository::getExternalId)
                .collect(Collectors.toSet());

        return clientFactory.forConnection(connection).listRepositories(search, page).stream()
                .map(info -> ScmRemoteRepoResponse.from(info, mappedExternalIds.contains(info.externalId())))
                .toList();
    }

    // ─── Paket kısıtları ──────────────────────────────────────────────────────

    private void checkConnectionLimit(UUID orgId) {
        String planCode = entitlementService.getEntitlements(orgId).planCode();
        Integer limit = CONNECTION_LIMITS.get(planCode);
        if (limit != null && connectionRepository.countByOrganizationId(orgId) >= limit) {
            throw new PlanLimitExceededException("SCM_CONNECTIONS",
                    "Paketinizin SCM bağlantı limitine ulaştınız (" + limit + " bağlantı). " +
                    "Daha fazla bağlantı için paketinizi yükseltin.");
        }
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private ScmConnection getConnection(UUID orgId, UUID connectionId) {
        return connectionRepository.findByIdAndOrganizationId(connectionId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("SCM bağlantısı bulunamadı."));
    }

    private void checkAdminAccess(UUID orgId, String email) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(orgId, email,
                List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN))) {
            throw new SecurityException("Bu işlem için organizasyon yöneticisi yetkisi gerekli.");
        }
    }

    private void requireCryptoConfigured() {
        if (!tokenCrypto.isEnabled()) {
            throw new IllegalStateException(
                    "Git entegrasyonu bu sunucuda yapılandırılmamış (SCM_CRYPTO_KEY tanımlı değil).");
        }
    }

    private void removeWebhookQuietly(ScmConnection connection, ScmRepository repo) {
        if (repo.getWebhookExternalId() == null) return;
        try {
            clientFactory.forConnection(connection).removeWebhook(repo, repo.getWebhookExternalId());
        } catch (Exception e) {
            log.warn("Webhook silinemedi (repo={}): {}", repo.getFullName(), e.getMessage());
        }
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) return null;
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

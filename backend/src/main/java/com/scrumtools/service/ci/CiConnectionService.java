package com.scrumtools.service.ci;

import com.scrumtools.dto.CiConnectionRequest;
import com.scrumtools.dto.CiConnectionResponse;
import com.scrumtools.dto.CiConnectionTestResponse;
import com.scrumtools.dto.CiJobInfoResponse;
import com.scrumtools.dto.CiJobParameterResponse;
import com.scrumtools.entity.CiConnection;
import com.scrumtools.entity.CiJobMapping;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.enums.CiConnectionStatus;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.exception.PlanLimitExceededException;
import com.scrumtools.repository.CiBuildRepository;
import com.scrumtools.repository.CiConnectionRepository;
import com.scrumtools.repository.CiJobMappingRepository;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.service.EntitlementService;
import com.scrumtools.service.ci.client.CiApiException;
import com.scrumtools.service.ci.client.CiClient;
import com.scrumtools.service.ci.client.CiClientFactory;
import com.scrumtools.service.ci.client.CiServerInfo;
import com.scrumtools.service.scm.ScmTokenCrypto;
import com.scrumtools.service.scm.ScmUrlValidator;
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
 * Org seviyesi CI/CD bağlantılarının yönetimi (CRUD + test + canlı job keşfi).
 * Tüm işlemler ORG_OWNER/ORG_ADMIN yetkisi ve CI_CD_INTEGRATION entitlement'ı ister.
 * ScmConnectionService ile aynı deseni izler; token şifrelemesi de aynı anahtarı kullanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CiConnectionService {

    /** Plan koduna göre bağlantı sayısı limiti; listede olmayan plan = sınırsız. */
    private static final Map<String, Integer> CONNECTION_LIMITS = Map.of("PRO", 1);

    private final CiConnectionRepository connectionRepository;
    private final CiJobMappingRepository jobMappingRepository;
    private final CiBuildRepository buildRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EntitlementService entitlementService;
    private final ScmTokenCrypto tokenCrypto;
    private final ScmUrlValidator urlValidator;
    private final CiClientFactory clientFactory;

    @Transactional(readOnly = true)
    public List<CiConnectionResponse> list(UUID orgId, String email) {
        checkAdminAccess(orgId, email);
        return connectionRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId).stream()
                .map(c -> CiConnectionResponse.from(c, jobMappingRepository.countByConnectionId(c.getId())))
                .toList();
    }

    @Transactional
    public CiConnectionResponse create(UUID orgId, String email, CiConnectionRequest request) {
        checkAdminAccess(orgId, email);
        requireCryptoConfigured();
        entitlementService.assertFeature(orgId, PlanFeature.CI_CD_INTEGRATION);
        checkConnectionLimit(orgId);

        if (request.provider() == null) throw new IllegalArgumentException("Sağlayıcı seçilmeli.");
        if (isBlank(request.name())) throw new IllegalArgumentException("Bağlantı adı zorunlu.");
        if (isBlank(request.baseUrl())) throw new IllegalArgumentException("Jenkins adresi zorunlu.");
        if (isBlank(request.username())) throw new IllegalArgumentException("Kullanıcı adı zorunlu.");
        if (isBlank(request.token())) throw new IllegalArgumentException("API token zorunlu.");

        String baseUrl = normalizeBaseUrl(request.baseUrl());
        urlValidator.validate(baseUrl);

        // Kimlik bilgileri kaydedilmeden önce sunucuda doğrulanır — geçersizse hata döner
        CiClient client = clientFactory.forRawToken(
                request.provider(), baseUrl, request.username().trim(), request.token().trim());
        CiServerInfo serverInfo = client.validateConnection();

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));

        CiConnection connection = CiConnection.builder()
                .organization(org)
                .provider(request.provider())
                .name(request.name().trim())
                .baseUrl(baseUrl)
                .username(request.username().trim())
                .encryptedToken(tokenCrypto.encrypt(request.token().trim()))
                .tokenHint(tokenCrypto.hint(request.token().trim()))
                .webhookSecret(generateWebhookSecret())
                .crumbEnabled(serverInfo.crumbRequired())
                .serverVersion(serverInfo.version())
                .status(CiConnectionStatus.ACTIVE)
                .createdBy(email)
                .lastValidatedAt(LocalDateTime.now())
                .build();

        connection = connectionRepository.save(connection);
        log.info("CI/CD bağlantısı eklendi: org={} provider={} name='{}'",
                org.getSlug(), request.provider(), connection.getName());
        return CiConnectionResponse.from(connection, 0);
    }

    @Transactional
    public CiConnectionResponse update(UUID orgId, String email, UUID connectionId, CiConnectionRequest request) {
        checkAdminAccess(orgId, email);
        CiConnection connection = getConnection(orgId, connectionId);

        if (!isBlank(request.name())) connection.setName(request.name().trim());
        if (!isBlank(request.baseUrl())) {
            String baseUrl = normalizeBaseUrl(request.baseUrl());
            urlValidator.validate(baseUrl);
            connection.setBaseUrl(baseUrl);
        }
        if (!isBlank(request.username())) connection.setUsername(request.username().trim());

        boolean tokenChanged = !isBlank(request.token());
        if (tokenChanged) {
            requireCryptoConfigured();
            connection.setEncryptedToken(tokenCrypto.encrypt(request.token().trim()));
            connection.setTokenHint(tokenCrypto.hint(request.token().trim()));
        }

        // Adres/kullanıcı/token'dan biri değiştiyse yeni bilgilerle doğrulanır
        if (tokenChanged || !isBlank(request.baseUrl()) || !isBlank(request.username())) {
            CiServerInfo serverInfo = clientFactory.forConnection(connection).validateConnection();
            connection.setCrumbEnabled(serverInfo.crumbRequired());
            connection.setServerVersion(serverInfo.version());
            connection.setStatus(CiConnectionStatus.ACTIVE);
            connection.setLastValidatedAt(LocalDateTime.now());
            connection.setConsecutiveFailures(0);
        }

        connection = connectionRepository.save(connection);
        return CiConnectionResponse.from(connection,
                jobMappingRepository.countByConnectionId(connection.getId()));
    }

    /** Bağlantıyı, job eşlemelerini ve build tarihçesini kaldırır. */
    @Transactional
    public void delete(UUID orgId, String email, UUID connectionId) {
        checkAdminAccess(orgId, email);
        CiConnection connection = getConnection(orgId, connectionId);

        List<CiJobMapping> mappings = jobMappingRepository.findByConnectionId(connectionId);
        for (CiJobMapping mapping : mappings) {
            buildRepository.deleteByJobMappingId(mapping.getId());
        }
        jobMappingRepository.deleteAll(mappings);
        connectionRepository.delete(connection);
        log.info("CI/CD bağlantısı silindi: {} ({} job eşlemesiyle birlikte)",
                connection.getName(), mappings.size());
    }

    /** Bağlantıyı canlı test eder; sonuç ne olursa olsun 200 döner (ok flag'i ile). */
    @Transactional
    public CiConnectionTestResponse test(UUID orgId, String email, UUID connectionId) {
        checkAdminAccess(orgId, email);
        CiConnection connection = getConnection(orgId, connectionId);
        try {
            CiServerInfo info = clientFactory.forConnection(connection).validateConnection();
            connection.setStatus(CiConnectionStatus.ACTIVE);
            connection.setCrumbEnabled(info.crumbRequired());
            connection.setServerVersion(info.version());
            connection.setLastValidatedAt(LocalDateTime.now());
            connection.setConsecutiveFailures(0);
            connectionRepository.save(connection);

            String who = info.user() != null ? info.user() : connection.getUsername();
            return new CiConnectionTestResponse(true,
                    "Bağlantı başarılı (" + who + ").", info.version(), info.user(), info.crumbRequired());
        } catch (CiApiException e) {
            if (e.isAuthFailure()) {
                connection.setStatus(CiConnectionStatus.INVALID);
                connectionRepository.save(connection);
            }
            return new CiConnectionTestResponse(false, e.getMessage(), null, null, false);
        }
    }

    /** Sunucudan CANLI job listesi — eşleme ekranı için (eşlenmişler işaretlenir). */
    @Transactional(readOnly = true)
    public List<CiJobInfoResponse> listJobs(UUID orgId, String email, UUID connectionId, String search) {
        checkAdminAccess(orgId, email);
        CiConnection connection = getConnection(orgId, connectionId);

        Set<String> mappedJobs = jobMappingRepository.findByConnectionId(connectionId).stream()
                .map(CiJobMapping::getJobFullName)
                .collect(Collectors.toSet());

        return clientFactory.forConnection(connection).listJobs(search).stream()
                .map(info -> CiJobInfoResponse.from(info, mappedJobs.contains(info.fullName())))
                .toList();
    }

    /** Seçilen job'ın tanımlı parametreleri — şablon editörünü ön doldurmak için. */
    @Transactional(readOnly = true)
    public List<CiJobParameterResponse> listJobParameters(UUID orgId, String email,
                                                          UUID connectionId, String jobFullName) {
        checkAdminAccess(orgId, email);
        if (isBlank(jobFullName)) throw new IllegalArgumentException("Job adı zorunlu.");
        CiConnection connection = getConnection(orgId, connectionId);

        return clientFactory.forConnection(connection).getJobParameters(jobFullName.trim()).stream()
                .map(CiJobParameterResponse::from)
                .toList();
    }

    // ─── Paket kısıtları ──────────────────────────────────────────────────────

    private void checkConnectionLimit(UUID orgId) {
        String planCode = entitlementService.getEntitlements(orgId).planCode();
        Integer limit = CONNECTION_LIMITS.get(planCode);
        if (limit != null && connectionRepository.countByOrganizationId(orgId) >= limit) {
            throw new PlanLimitExceededException("CI_CONNECTIONS",
                    "Paketinizin CI/CD bağlantı limitine ulaştınız (" + limit + " bağlantı). " +
                    "Daha fazla bağlantı için paketinizi yükseltin.");
        }
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    /** Diğer CI servislerinin ortak erişim noktası (bağlantı org'a ait mi kontrolü dahil). */
    CiConnection getConnection(UUID orgId, UUID connectionId) {
        return connectionRepository.findByIdAndOrganizationId(connectionId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("CI/CD bağlantısı bulunamadı."));
    }

    void checkAdminAccess(UUID orgId, String email) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(orgId, email,
                List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN))) {
            throw new SecurityException("Bu işlem için organizasyon yöneticisi yetkisi gerekli.");
        }
    }

    private void requireCryptoConfigured() {
        if (!tokenCrypto.isEnabled()) {
            throw new IllegalStateException(
                    "CI/CD entegrasyonu bu sunucuda yapılandırılmamış (SCM_CRYPTO_KEY tanımlı değil).");
        }
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

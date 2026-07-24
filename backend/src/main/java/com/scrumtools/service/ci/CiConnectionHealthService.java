package com.scrumtools.service.ci;

import com.scrumtools.entity.CiConnection;
import com.scrumtools.entity.OrganizationMember;
import com.scrumtools.entity.enums.CiConnectionStatus;
import com.scrumtools.entity.enums.NotificationType;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.repository.CiConnectionRepository;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Poller'ın bağlantı sağlık durumunu yönettiği yardımcı servis (poller'dan ayrı
 * transaction'larda çalışır). Ardışık ulaşılamama/kimlik hatası eşiği aşınca bağlantı
 * INVALID işaretlenir ve org yöneticilerine bildirim düşülür; başarılı sorguda sayaç sıfırlanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CiConnectionHealthService {

    /** Bu kadar ardışık hatadan sonra bağlantı INVALID'e çekilir (plan 5.3). */
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final CiConnectionRepository connectionRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final NotificationService notificationService;

    /** Başarılı sorgudan sonra — sayaç sıfırlanır, gerekiyorsa ACTIVE'e döner. */
    @Transactional
    public void recordSuccess(UUID connectionId) {
        connectionRepository.findById(connectionId).ifPresent(connection -> {
            boolean changed = false;
            if (connection.getConsecutiveFailures() != 0) {
                connection.setConsecutiveFailures(0);
                changed = true;
            }
            // Poller kendiliğinden toparlanan bağlantıyı yeniden aktif eder
            if (connection.getStatus() == CiConnectionStatus.INVALID) {
                connection.setStatus(CiConnectionStatus.ACTIVE);
                changed = true;
            }
            if (changed) connectionRepository.save(connection);
        });
    }

    /**
     * Başarısız sorgudan sonra — sayaç artırılır. Kimlik hatasında anında,
     * ulaşılamamada eşik aşımında bağlantı INVALID işaretlenir.
     */
    @Transactional
    public void recordFailure(UUID connectionId, boolean authFailure) {
        connectionRepository.findById(connectionId).ifPresent(connection -> {
            if (connection.getStatus() == CiConnectionStatus.INVALID) return; // zaten işaretli

            int failures = connection.getConsecutiveFailures() + 1;
            connection.setConsecutiveFailures(failures);

            if (authFailure || failures >= MAX_CONSECUTIVE_FAILURES) {
                connection.setStatus(CiConnectionStatus.INVALID);
                connectionRepository.save(connection);
                notifyAdmins(connection, authFailure);
                log.warn("CI/CD bağlantısı INVALID işaretlendi: {} (authFailure={}, ardışık hata={})",
                        connection.getName(), authFailure, failures);
            } else {
                connectionRepository.save(connection);
            }
        });
    }

    private void notifyAdmins(CiConnection connection, boolean authFailure) {
        UUID orgId = connection.getOrganization().getId();
        String reason = authFailure
                ? "kimlik bilgileri reddedildi"
                : "sunucuya ardışık olarak ulaşılamadı";
        String message = "'" + connection.getName() + "' CI/CD bağlantısı devre dışı bırakıldı ("
                + reason + "). Build durum takibi durdu; bağlantıyı organizasyon ayarlarından gözden geçirin.";

        List<OrganizationMember> admins = organizationMemberRepository.findByOrganizationIdAndOrgRoleIn(
                orgId, List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN));
        Map<String, Object> data = Map.of("connectionId", connection.getId().toString(), "orgId", orgId.toString());

        for (OrganizationMember admin : admins) {
            if (admin.getUser() == null || admin.getUser().getEmail() == null) continue;
            notificationService.createAndPush(
                    admin.getUser().getEmail(),
                    NotificationType.SYSTEM,
                    "CI/CD bağlantısı devre dışı",
                    message,
                    "ci_connection",
                    connection.getId().toString(),
                    data);
        }
    }
}

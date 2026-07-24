package com.scrumtools.service.ci;

import com.scrumtools.entity.CiBuild;
import com.scrumtools.entity.CiJobMapping;
import com.scrumtools.entity.enums.CiBuildContext;
import com.scrumtools.entity.enums.CiBuildStatus;
import com.scrumtools.entity.enums.CiJobType;
import com.scrumtools.repository.CiBuildRepository;
import com.scrumtools.repository.ReleaseRepository;
import com.scrumtools.service.NotificationService;
import com.scrumtools.service.ReleaseService;
import com.scrumtools.service.ci.client.CiApiException;
import com.scrumtools.service.ci.client.CiBuildInfo;
import com.scrumtools.service.ci.client.CiClient;
import com.scrumtools.service.ci.client.CiQueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tek bir build'in durum makinesi: QUEUED → (kuyruk çözümü) → RUNNING → (build durumu) → terminal.
 * Poller ve manuel yenileme (refresh) ortak bu servisi çağırır.
 *
 * <p><b>Transaction ayrımı:</b> {@link #sync} terminal durumu ve bildirimleri kendi transaction'ında
 * yazar; başarı sonrası otomatik release geçişi ({@link #applyAutoTransition}) AYRI bir transaction'da,
 * çağıran taraftan (self-invocation değil) tetiklenir. Böylece geçiş servisi hata verse bile build'in
 * terminal durumu geri alınmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CiBuildSyncService {

    /** Kuyrukta bu süreyi aşan build'ler LOST kabul edilir (plan 5.3). */
    private static final long LOST_AFTER_MINUTES = 30;

    private final CiBuildRepository buildRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseService releaseService;
    private final NotificationService notificationService;

    /**
     * Build'i bir adım ilerletir. Ağ/kimlik hatası (CiApiException) YUKARI fırlatılır —
     * poller bağlantı sağlığını buna göre günceller; 404 (kayıp kayıt) içeride LOST'a çevrilir.
     *
     * @return otomatik release geçişi gerekiyorsa gereken primitifleri taşıyan sonuç, aksi halde {@link SyncResult#NONE}
     */
    @Transactional
    public SyncResult sync(UUID buildId, CiClient client) {
        CiBuild build = buildRepository.findById(buildId).orElse(null);
        if (build == null || build.getStatus().isTerminal()) return SyncResult.NONE;

        return switch (build.getStatus()) {
            case QUEUED -> syncQueued(build, client);
            case RUNNING -> syncRunning(build, client);
            default -> SyncResult.NONE;
        };
    }

    private SyncResult syncQueued(CiBuild build, CiClient client) {
        if (isBlank(build.getQueueItemUrl())) {
            finishTerminal(build, CiBuildStatus.LOST, "Kuyruk adresi alınamadı.", null);
            return SyncResult.NONE;
        }

        CiQueueStatus status;
        try {
            status = client.getQueueStatus(build.getQueueItemUrl());
        } catch (CiApiException e) {
            if (e.getStatusCode() == 404) { // kuyruk kaydı düşmüş, build no çözülemedi
                finishTerminal(build, CiBuildStatus.LOST, "Kuyruk kaydı bulunamadı (süresi dolmuş).", null);
                return SyncResult.NONE;
            }
            throw e;
        }

        if (status.cancelled()) {
            finishTerminal(build, CiBuildStatus.ABORTED, "Build kuyruktayken iptal edildi.", null);
            return SyncResult.NONE;
        }
        if (status.isResolved()) {
            build.setBuildNumber(status.buildNumber());
            build.setBuildUrl(status.buildUrl());
            build.setStatus(CiBuildStatus.RUNNING);
            build.setStartedAt(LocalDateTime.now());
            buildRepository.save(build);
            return SyncResult.NONE;
        }
        // Hâlâ kuyrukta — makul süreyi aştıysa LOST
        if (build.getTriggeredAt() != null
                && build.getTriggeredAt().isBefore(LocalDateTime.now().minusMinutes(LOST_AFTER_MINUTES))) {
            finishTerminal(build, CiBuildStatus.LOST,
                    "Kuyrukta " + LOST_AFTER_MINUTES + " dakikayı aştı, build başlatılamadı.", null);
        }
        return SyncResult.NONE;
    }

    private SyncResult syncRunning(CiBuild build, CiClient client) {
        if (isBlank(build.getBuildUrl())) {
            finishTerminal(build, CiBuildStatus.LOST, "Build adresi alınamadı.", null);
            return SyncResult.NONE;
        }

        CiBuildInfo info;
        try {
            info = client.getBuildInfo(build.getBuildUrl());
        } catch (CiApiException e) {
            if (e.getStatusCode() == 404) {
                finishTerminal(build, CiBuildStatus.LOST, "Build kaydı Jenkins'te bulunamadı.", null);
                return SyncResult.NONE;
            }
            throw e;
        }

        if (info.status() == CiBuildStatus.RUNNING) {
            return SyncResult.NONE; // devam ediyor
        }
        finishTerminal(build, info.status(), terminalMessage(info.status()), info.durationMs());

        // Otomatik geçiş yalnız RELEASE_PIPELINE + SUCCESS + bayrak açıkken; primitifleri tx içinde topla
        CiJobMapping mapping = build.getJobMapping();
        boolean autoTransition = mapping.getJobType() == CiJobType.RELEASE_PIPELINE
                && info.status() == CiBuildStatus.SUCCESS
                && Boolean.TRUE.equals(mapping.getAutoTransitionOnSuccess())
                && build.getReleaseId() != null;
        if (!autoTransition) return SyncResult.NONE;

        return new SyncResult(true, mapping.getProject().getId(),
                build.getReleaseId(), build.getReleaseName(), build.getTriggeredBy());
    }

    private void finishTerminal(CiBuild build, CiBuildStatus status, String message, Long durationMs) {
        LocalDateTime now = LocalDateTime.now();
        build.setStatus(status);
        build.setFinishedAt(now);
        build.setStatusMessage(message);
        if (durationMs != null && durationMs > 0) {
            build.setDurationMs(durationMs);
        } else if (build.getStartedAt() != null) {
            build.setDurationMs(Duration.between(build.getStartedAt(), now).toMillis());
        }
        buildRepository.save(build);
        notifyTerminal(build);
    }

    private void notifyTerminal(CiBuild build) {
        boolean success = build.getStatus() == CiBuildStatus.SUCCESS;
        String subject = build.getContextType() == CiBuildContext.RELEASE
                ? build.getReleaseName() : build.getTaskCustomId();
        String job = build.getJobMapping().getDisplayName();
        String statusLabel = statusLabel(build.getStatus());
        String buildIdStr = build.getId().toString();

        // Tetikleyen kullanıcı
        notificationService.notifyCiBuild(build.getTriggeredBy(), success, build.getContextType().name(),
                subject, job, statusLabel, build.getBuildUrl(), buildIdStr);

        // Release ise ayrıca release manager (tetikleyenden farklıysa)
        if (build.getContextType() == CiBuildContext.RELEASE && build.getReleaseId() != null) {
            releaseRepository.findById(build.getReleaseId()).ifPresent(release -> {
                if (release.getReleaseManager() == null) return;
                String mgr = release.getReleaseManager().getEmail();
                if (mgr != null && !mgr.equalsIgnoreCase(build.getTriggeredBy())) {
                    notificationService.notifyCiBuild(mgr, success, build.getContextType().name(),
                            subject, job, statusLabel, build.getBuildUrl(), buildIdStr);
                }
            });
        }
    }

    /**
     * Otomatik release geçişi — {@link #sync} sonucunda gerekiyorsa çağrılır.
     * Bilinçli olarak transaction AÇMAZ: {@link ReleaseService#updateStatus} kendi transaction'ını
     * yönetir; hata verirse yalnız o geçiş geri alınır, build'in (ayrı transaction'da yazılmış)
     * SUCCESS durumu korunur ve poller aynı build'i tekrar denemez. Geçiş, build'i tetikleyen
     * kullanıcı adına yapılır (tetiklemede release manager/org admin yetkisi zaten doğrulanmıştı).
     */
    public void applyAutoTransition(SyncResult result) {
        if (result == null || !result.needsAutoTransition()) return;
        try {
            releaseService.updateStatus(result.projectId(), result.releaseId(), "RELEASED", result.triggeredBy());
            log.info("Release build başarısıyla otomatik RELEASED'a geçirildi: release='{}'", result.releaseName());
        } catch (Exception e) {
            log.warn("Otomatik RELEASED geçişi yapılamadı (release='{}'): {}",
                    result.releaseName(), e.getMessage());
        }
    }

    /**
     * {@link #sync} sonucu — terminal SUCCESS'te otomatik release geçişi için gereken primitifler
     * (transaction içinde toplanır, dışında güvenle kullanılır; lazy erişim gerekmez).
     */
    public record SyncResult(boolean needsAutoTransition, UUID projectId, UUID releaseId,
                             String releaseName, String triggeredBy) {
        public static final SyncResult NONE = new SyncResult(false, null, null, null, null);
    }

    // ─── Metin yardımcıları ───────────────────────────────────────────────────

    private String terminalMessage(CiBuildStatus status) {
        return switch (status) {
            case SUCCESS -> "Build başarıyla tamamlandı.";
            case FAILURE -> "Build başarısız oldu.";
            case UNSTABLE -> "Build kararsız tamamlandı (testler uyarı verdi).";
            case ABORTED -> "Build iptal edildi.";
            default -> null;
        };
    }

    private String statusLabel(CiBuildStatus status) {
        return switch (status) {
            case SUCCESS -> "başarılı";
            case FAILURE -> "başarısız";
            case UNSTABLE -> "kararsız";
            case ABORTED -> "iptal edildi";
            case LOST -> "kayıp";
            default -> status.name().toLowerCase();
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

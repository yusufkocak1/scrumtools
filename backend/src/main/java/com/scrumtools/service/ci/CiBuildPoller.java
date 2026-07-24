package com.scrumtools.service.ci;

import com.scrumtools.entity.CiBuild;
import com.scrumtools.entity.CiConnection;
import com.scrumtools.entity.enums.CiBuildStatus;
import com.scrumtools.entity.enums.CiConnectionStatus;
import com.scrumtools.repository.CiBuildRepository;
import com.scrumtools.service.ci.client.CiApiException;
import com.scrumtools.service.ci.client.CiClient;
import com.scrumtools.service.ci.client.CiClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aktif build'lerin (QUEUED/RUNNING) durumunu Jenkins'ten periyodik senkronize eder.
 * Yalnız açık build'i olan bağlantılar sorgulanır; her bağlantı için tek client örneği
 * kurulur ve o bağlantının build'leri sırayla ilerletilir.
 *
 * <p>Bağlantı ulaşılamaz/kimlik hatası verirse o bağlantının kalan build'leri bu turda
 * atlanır ve {@link CiConnectionHealthService} ile sağlık durumu güncellenir (eşik aşımında
 * INVALID). Webhook fazı eklendiğinde webhook'la güncellenen build'ler zaten terminal olacağı
 * için poller onları görmez — iki mekanizma çakışmaz.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CiBuildPoller {

    private static final List<CiBuildStatus> ACTIVE = List.of(CiBuildStatus.QUEUED, CiBuildStatus.RUNNING);

    private final CiBuildRepository buildRepository;
    private final CiClientFactory clientFactory;
    private final CiBuildSyncService syncService;
    private final CiConnectionHealthService healthService;

    @Scheduled(fixedDelay = 20_000, initialDelay = 30_000)
    public void poll() {
        List<CiBuild> active = buildRepository.findActiveWithConnection(ACTIVE);
        if (active.isEmpty()) return;

        // Bağlantı bazında grupla (ekleme sırası korunur — client bir kez kurulur)
        Map<UUID, List<CiBuild>> byConnection = new LinkedHashMap<>();
        Map<UUID, CiConnection> connections = new LinkedHashMap<>();
        for (CiBuild build : active) {
            CiConnection connection = build.getJobMapping().getConnection();
            connections.putIfAbsent(connection.getId(), connection);
            byConnection.computeIfAbsent(connection.getId(), k -> new java.util.ArrayList<>()).add(build);
        }

        for (Map.Entry<UUID, List<CiBuild>> entry : byConnection.entrySet()) {
            CiConnection connection = connections.get(entry.getKey());
            if (connection.getStatus() != CiConnectionStatus.ACTIVE) {
                continue; // INVALID/DISABLED bağlantılar atlanır (test/güncelleme yeniden aktifleştirir)
            }
            processConnection(connection, entry.getValue());
        }
    }

    /** Tek bağlantının build'lerini ilerletir; ilk ulaşılamama/kimlik hatasında turu keser. */
    private void processConnection(CiConnection connection, List<CiBuild> builds) {
        CiClient client = clientFactory.forConnection(connection);
        boolean anySuccess = false;

        for (CiBuild build : builds) {
            try {
                CiBuildSyncService.SyncResult result = syncService.sync(build.getId(), client);
                anySuccess = true;
                syncService.applyAutoTransition(result);
            } catch (CiApiException e) {
                if (e.isAuthFailure() || e.isUnreachable()) {
                    // Bağlantı geneli sorun — sağlığı güncelle, bu bağlantının kalanını sonraki tura bırak
                    healthService.recordFailure(connection.getId(), e.isAuthFailure());
                    log.debug("CI poll bağlantı hatası ({}), tur kesildi: {}",
                            connection.getName(), e.getMessage());
                    return;
                }
                // Build'e özel hata (ör. beklenmedik 4xx) — bu build'i atla, diğerleri sürsün
                log.warn("CI poll build hatası (build={}): {}", build.getId(), e.getMessage());
            } catch (Exception e) {
                log.warn("CI poll beklenmedik hata (build={}): {}", build.getId(), e.getMessage());
            }
        }

        if (anySuccess) {
            healthService.recordSuccess(connection.getId());
        }
    }
}

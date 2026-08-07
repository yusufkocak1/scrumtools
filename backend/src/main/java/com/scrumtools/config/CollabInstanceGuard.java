package com.scrumtools.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tek backend örneği kuralını görünür kılar (COLLAB_WORKSPACE_PLAN.md K9).
 *
 * <p>Ortak çalışma oturumları ve STOMP broker'ı süreç-içidir (plan D3: Redis yok).
 * İkinci bir backend örneği açılırsa hata alınmaz — sessizce yanlış çalışır:
 * A örneğine bağlı kullanıcı B örneğine bağlı kullanıcının düzenlemesini görmez,
 * iki taraf da kendi anlık görüntüsünü yazar. Bu tür bir arıza üretimde saatlerce
 * fark edilmeyebileceği için hem açılışta hem {@code /actuator/health} üzerinde
 * açıkça raporlanır.
 *
 * <p>{@code docker-compose.yml}'daki {@code container_name} zaten
 * {@code --scale backend=N} çağrısını hata verdirir; buradaki kontrol o tesadüfi
 * korumanın yerini almaz, onu <i>bilinçli</i> hâle getirir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabInstanceGuard implements HealthIndicator {

    private final CollabProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void logInstanceMode() {
        if (properties.isSingleInstance()) {
            log.info("Collab: tek örnek modu etkin — ortak çalışma oturumları süreç-içi tutuluyor. "
                    + "Backend YATAY ÖLÇEKLENEMEZ (bkz. COLLAB_WORKSPACE_PLAN.md K9).");
        } else {
            log.warn("Collab: app.collab.single-instance=false. Bu bayrak yatay ölçeklemeyi MÜMKÜN KILMAZ; "
                    + "süreç-içi broker ve oturum kaydı hâlâ tek örnek varsayıyor. Birden fazla backend "
                    + "örneği çalışıyorsa kullanıcılar birbirinin düzenlemesini göremez.");
        }
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("singleInstance", properties.isSingleInstance())
                .withDetail("horizontalScaling", "unsupported")
                .withDetail("appendBatchWindowMs", properties.getAppendBatchWindowMs())
                .build();
    }
}

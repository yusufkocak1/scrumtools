package com.scrumtools.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate, enum (EnumType.STRING) kolonlar için tabloyu ilk oluştururken
 * CHECK (kolon IN (...)) kısıtı üretir; ddl-auto:update sonradan enum'a eklenen
 * değerleri bu kısıta YANSITMAZ. Yeni enum değeri yazılmaya çalışıldığında
 * "violates check constraint" hatası oluşur (bkz. organizations_plan_check örneği).
 *
 * Çözüm: eskimiş kısıtları açılışta düşürmek. Enum doğrulaması zaten uygulama
 * katmanında (Java enum binding) yapıldığı için DB kısıtı gereksizdir.
 * DROP IF EXISTS idempotent olduğundan her boot'ta güvenle çalışır.
 */
@Component
@Order(0) // Diğer runner'lar bildirim üretmeden önce çalışsın
@RequiredArgsConstructor
@Slf4j
public class SchemaConstraintFixRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // NotificationType enum'una SUPPORT_TICKET_* değerleri eklendi;
        // eski kısıt bu değerleri tanımadığı için bildirim insert'leri patlıyordu.
        dropConstraint("notifications", "notifications_type_check");

        // Git ve CI/CD entegrasyonu eklenirken genişleyen enum'lar:
        // Permission.SCM_CREATE_BRANCH + SCM_CREATE_PULL_REQUEST,
        // PlanFeature.GIT_INTEGRATION + CI_CD_INTEGRATION, ActivityAction.SCM_*
        // DataInitializer.backfillScmGrants bu değerleri mevcut kayıtlara yazar —
        // eski CHECK kısıtları düşürülmezse boot'ta insert patlar.
        dropConstraint("role_permissions", "role_permissions_permission_check");
        dropConstraint("plan_features", "plan_features_feature_check");
        dropConstraint("activity_events", "activity_events_action_check");

        // Adam Asmaca kelimeleri takım bazlıyken global'e çevrildi; entity'de team_id
        // kalmadı ama ddl-auto:update kolonu düşürmediği için NOT NULL kısıtı insert'leri
        // patlatıyordu. Kolon artık ölü — verisi korunsun diye sadece NOT NULL kaldırılıyor.
        dropNotNull("hangman_words", "team_id");
    }

    private void dropConstraint(String table, String constraint) {
        jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
        log.info("Eskimiş CHECK kısıtı kontrol edildi/düşürüldü: {}.{}", table, constraint);
    }

    /** Kolon yoksa sessizce geçer; ALTER COLUMN'un IF EXISTS karşılığı yok. */
    private void dropNotNull(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " DROP NOT NULL");
            log.info("Eskimiş NOT NULL kısıtı düşürüldü: {}.{}", table, column);
        } catch (Exception e) {
            log.debug("NOT NULL düşürülemedi (kolon yok olabilir): {}.{} — {}", table, column, e.getMessage());
        }
    }
}

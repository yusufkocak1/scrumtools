package com.scrumtools.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Code Share → Collab Workspace temiz kesimi (COLLAB_WORKSPACE_PLAN.md — D1 / K10).
 *
 * <p><b>Neden native SQL ve neden en yüksek öncelik:</b> {@code PlanFeature.CODE_SHARE}
 * sabiti {@code plan_features} tablosunda {@code EnumType.STRING} olarak saklanıyor.
 * Sabit enum'dan silindiği için, o değeri taşıyan satırlar JPA ile okunmaya
 * çalışıldığında uygulama açılışta patlar. {@link JdbcTemplate} enum
 * dönüştürücüsünden geçmediğinden, satırları herhangi bir JPA erişiminden önce
 * burada yeni değere çevirebiliyoruz.
 *
 * <p>Hepsi idempotenttir; her boot'ta güvenle çalışır ve bir kez göç ettikten sonra
 * hiçbir satıra dokunmaz. Göç tamamen yayıldığında bu sınıf silinebilir.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class CollabCleanupRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        renamePlanFeature();
        dropCodeShareTable();
    }

    /**
     * CODE_SHARE → COLLAB_WORKSPACE.
     *
     * <p>Önce CHECK kısıtı düşürülür: Hibernate tabloyu ilk kurarken
     * {@code feature IN (...)} kısıtını üretir ve {@code ddl-auto:update} sonradan
     * eklenen enum değerlerini bu kısıta yansıtmaz — düşürülmezse UPDATE patlar.
     * ({@code SchemaConstraintFixRunner} aynı kısıtı düşürüyor ama o {@code @Order(0)},
     * yani bu runner'dan sonra çalışıyor.)
     */
    private void renamePlanFeature() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE plan_features DROP CONSTRAINT IF EXISTS plan_features_feature_check");
            int updated = jdbcTemplate.update(
                    "UPDATE plan_features SET feature = 'COLLAB_WORKSPACE' WHERE feature = 'CODE_SHARE'");
            if (updated > 0) {
                log.info("{} plan özelliği CODE_SHARE → COLLAB_WORKSPACE olarak göç ettirildi.", updated);
            }
        } catch (Exception e) {
            // İlk kurulumda tablo henüz yok — göç edecek satır da yok.
            log.debug("plan_features göçü atlandı: {}", e.getMessage());
        }
    }

    /**
     * {@code ddl-auto: update} entity silinince tabloyu kendiliğinden düşürmez.
     * D1 gereği veri taşınmıyor, tablo tamamen kaldırılır.
     */
    private void dropCodeShareTable() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS code_shares");
            log.info("code_shares tablosu kontrol edildi/düşürüldü (Code Share modülü kaldırıldı).");
        } catch (Exception e) {
            log.warn("code_shares tablosu düşürülemedi: {}", e.getMessage());
        }
    }
}

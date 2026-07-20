package com.scrumtools.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task-proje bağı migrasyonu: task'lar artık takıma değil projeye ait (customId
 * proje anahtarıyla üretilir). ddl-auto:update project_id kolonunu ekler ama
 * mevcut satırları dolduramaz; projesiz task'lar takımlarının bağlı olduğu
 * projeye atanır. Mevcut customId'ler korunur (git branch eşleştirmeleri ve
 * dış linkler kırılmasın diye). Idempotent — sadece project_id IS NULL satırları
 * günceller, her boot'ta güvenle çalışır.
 *
 * Ayrıca çoklu proje geçişi: takım-proje bağı tekil FK (teams.project_id) olmaktan
 * çıkıp team_projects join tablosuna taşındı (bir takım birden fazla projede
 * çalışabilir). teams.project_id birincil/varsayılan proje olarak korunur; mevcut
 * tekil bağlar join tablosuna kopyalanır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskProjectBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1) Mevcut tekil takım-proje bağlarını çoklu bağ tablosuna taşı.
        //    NOT EXISTS ile idempotent — join tablosunda PK/unique kısıt olmasa da tekrar etmez.
        int linked = jdbcTemplate.update(
                "INSERT INTO team_projects (team_id, project_id) " +
                "SELECT tm.id, tm.project_id FROM teams tm " +
                "WHERE tm.project_id IS NOT NULL " +
                "  AND NOT EXISTS (SELECT 1 FROM team_projects tp " +
                "                  WHERE tp.team_id = tm.id AND tp.project_id = tm.project_id)");
        if (linked > 0) {
            log.info("Çoklu proje migrasyonu: {} takım-proje bağı team_projects tablosuna taşındı.", linked);
        }

        // 2) Projesiz görevleri takımın birincil projesine ata.
        int updated = jdbcTemplate.update(
                "UPDATE tasks t SET project_id = tm.project_id " +
                "FROM teams tm " +
                "WHERE t.team_id = tm.id AND t.project_id IS NULL AND tm.project_id IS NOT NULL");
        if (updated > 0) {
            log.info("Task-proje migrasyonu: {} görev, takımının birincil projesine atandı.", updated);
        }

        // 3) Birincil projesi olmayan ama team_projects üzerinden projeye bağlı takımların
        //    görevleri — takımın ilk (en eski) projesine düşer.
        int fallback = jdbcTemplate.update(
                "UPDATE tasks t SET project_id = first_project.project_id " +
                "FROM (SELECT DISTINCT ON (tp.team_id) tp.team_id, tp.project_id " +
                "      FROM team_projects tp JOIN projects p ON p.id = tp.project_id " +
                "      ORDER BY tp.team_id, p.created_at) AS first_project " +
                "WHERE t.team_id = first_project.team_id AND t.project_id IS NULL");
        if (fallback > 0) {
            log.info("Task-proje migrasyonu: {} görev, takımının ilk projesine atandı.", fallback);
        }
    }
}

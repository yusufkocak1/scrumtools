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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskProjectBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = jdbcTemplate.update(
                "UPDATE tasks t SET project_id = tm.project_id " +
                "FROM teams tm " +
                "WHERE t.team_id = tm.id AND t.project_id IS NULL AND tm.project_id IS NOT NULL");
        if (updated > 0) {
            log.info("Task-proje migrasyonu: {} görev, takımının bağlı olduğu projeye atandı.", updated);
        }
    }
}

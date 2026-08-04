package com.scrumtools.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumtools.entity.Dashboard;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.DashboardVisibility;
import com.scrumtools.repository.DashboardRepository;
import com.scrumtools.repository.TeamRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code user_dashboards} → {@code dashboards} göçü.
 *
 * Eski model kullanıcı başına tek düzendi ve takım bilgisi yalnızca widget'ların
 * içinde duruyordu. Yeni pano takıma bağlı olduğu için takım oradan çıkarılır:
 * düzendeki ilk widget'ın {@code teamId} alanı, yoksa kullanıcının ilk takımı.
 * İkisi de yoksa satır atlanır — kullanıcının hangi takıma ait olduğunu
 * uydurmaktansa panoyu kurmamak yeğdir; ilk girişte varsayılan pano zaten kurulur.
 *
 * Genişlik alanı ({@code w}) eski düzende yoktu; hepsi 4 sütuna (12'lik ızgarada
 * üçte bir) ayarlanır — eski ekranın {@code xl:grid-cols-3} görünümünün birebir
 * karşılığı, böylece göç sonrası pano kullanıcının bıraktığı gibi açılır.
 *
 * Idempotent: kullanıcının o takımda zaten panosu varsa dokunulmaz.
 * Eski tablo silinmez — göç yanlış giderse veri elde kalsın.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DashboardMigrationRunner implements ApplicationRunner {

    private static final String MIGRATED_NAME = "Panom";
    /** Eski düzen üç sütunlu ızgaradaydı; 12'lik ızgarada karşılığı 4 sütun. */
    private static final int LEGACY_WIDTH = 4;

    private final JdbcTemplate jdbcTemplate;
    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!legacyTableExists()) return;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id, layout::text AS layout_json FROM user_dashboards");
        if (rows.isEmpty()) return;

        int migrated = 0;
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            try {
                if (migrateRow(row)) migrated++;
                else skipped++;
            } catch (Exception e) {
                skipped++;
                log.warn("Pano göçü atlandı (user={}): {}", row.get("user_id"), e.getMessage());
            }
        }
        if (migrated > 0 || skipped > 0) {
            log.info("Pano göçü: {} pano taşındı, {} satır atlandı.", migrated, skipped);
        }
    }

    /** @return göç edildiyse true; zaten var / takım çözülemedi ise false. */
    private boolean migrateRow(Map<String, Object> row) throws Exception {
        UUID userId = (UUID) row.get("user_id");
        if (userId == null) return false;

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) return false;

        List<Map<String, Object>> layout = readLayout((String) row.get("layout_json"));
        Optional<Team> team = resolveTeam(layout, userId);
        if (team.isEmpty()) return false;

        // Idempotency: bu kullanıcının o takımda panosu varsa göç zaten yapılmış
        // (ya da kullanıcı yeni model üzerinden kendi panosunu kurmuş) demektir.
        if (dashboardRepository.countByTeamIdAndOwnerId(team.get().getId(), userId) > 0) return false;

        dashboardRepository.save(Dashboard.builder()
                .owner(user.get())
                .team(team.get())
                .name(MIGRATED_NAME)
                .visibility(DashboardVisibility.PRIVATE)
                .layout(withLegacyWidth(layout))
                .position(0)
                .build());
        return true;
    }

    private List<Map<String, Object>> readLayout(String json) throws Exception {
        if (json == null || json.isBlank()) return new ArrayList<>();
        return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
    }

    private List<Map<String, Object>> withLegacyWidth(List<Map<String, Object>> layout) {
        List<Map<String, Object>> out = new ArrayList<>(layout.size());
        for (Map<String, Object> widget : layout) {
            if (widget == null || widget.get("id") == null || widget.get("type") == null) continue;
            widget.putIfAbsent("w", LEGACY_WIDTH);
            out.add(widget);
        }
        return out;
    }

    /** Takım: önce düzendeki ilk widget'ın takımı, olmazsa kullanıcının ilk takımı. */
    private Optional<Team> resolveTeam(List<Map<String, Object>> layout, UUID userId) {
        for (Map<String, Object> widget : layout) {
            if (widget == null) continue;
            Optional<Team> team = teamById(widget.get("teamId"));
            if (team.isPresent()) return team;
        }
        List<UUID> teamIds = jdbcTemplate.queryForList(
                "SELECT team_id FROM team_members WHERE user_id = ? LIMIT 1", UUID.class, userId);
        return teamIds.isEmpty() ? Optional.empty() : teamRepository.findById(teamIds.get(0));
    }

    private Optional<Team> teamById(Object rawId) {
        if (!(rawId instanceof String text) || text.isBlank()) return Optional.empty();
        try {
            return teamRepository.findById(UUID.fromString(text));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Eski tablo artık bir entity'ye bağlı değil; temiz kurulumlarda hiç oluşmaz.
     * {@code to_regclass} arama yoluna (search_path) göre çözer — uygulama kendi
     * şemasında çalıştığı için şema adı sabitlenmez.
     */
    private boolean legacyTableExists() {
        try {
            String name = jdbcTemplate.queryForObject(
                    "SELECT to_regclass('user_dashboards')::text", String.class);
            return name != null;
        } catch (Exception e) {
            log.debug("Eski pano tablosu kontrol edilemedi: {}", e.getMessage());
            return false;
        }
    }
}

package com.scrumtools.service;

import com.scrumtools.dto.DashboardRequest;
import com.scrumtools.dto.DashboardResponse;
import com.scrumtools.entity.Dashboard;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.DashboardVisibility;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.repository.DashboardRepository;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.repository.TeamRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Pano yönetimi.
 *
 * Görünürlük {@link com.scrumtools.service.RichFilterService} ile aynı kalıptadır
 * (PRIVATE / TEAM), ancak düzenleme hakkı bir noktada ondan ayrılır: paylaşılan
 * bir panoyu organizasyon yöneticisi de düzenleyebilir. Zengin filtre bir tanımdır
 * ve başkasının altından değiştirilmesi ona bağlı bütün panoları bozar; pano ise
 * bir görüntüdür — takıma açılmış bir panonun bakımının sahibinin izniyle kilitli
 * kalması, kişi ayrıldığında panoyu ölü bırakırdı.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    /** Kişi başına takımda azami pano — kötüye kullanıma karşı uygulama tavanı. */
    private static final int MAX_DASHBOARDS_PER_USER = 20;

    /** İlk açılışta kurulan panonun adı. */
    private static final String DEFAULT_NAME = "Panom";

    private static final List<OrgRole> ADMIN_ROLES = List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN);

    private final DashboardRepository dashboardRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    // ─── Listeleme ────────────────────────────────────────────────────────────

    /**
     * Takımdaki panolar. Kullanıcının hiç panosu yoksa varsayılan pano kurulur —
     * bu yüzden okuma yolu yazabilir. Alternatifi, arayüzde "önce bir pano
     * oluşturun" boş ekranıydı; eskiden dashboard açan herkes doğrudan
     * widget'larını görüyordu, o davranışı bozmamak için varsayılan sunucuda kurulur.
     */
    @Transactional
    public List<DashboardResponse> list(UUID teamId) {
        User user = currentUser();
        Team team = requireTeamMembership(teamId, user);

        List<Dashboard> visible = dashboardRepository.findVisibleForUser(teamId, user.getId());
        if (visible.stream().noneMatch(d -> d.getOwner().getId().equals(user.getId()))) {
            visible = new ArrayList<>(visible);
            visible.add(0, createDefault(team, user));
        }

        boolean admin = isOrgAdmin(team, user);
        return visible.stream()
                .map(d -> DashboardResponse.from(d, user.getId(), canEdit(d, user, admin)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(UUID dashboardId) {
        User user = currentUser();
        Dashboard dashboard = requireVisible(dashboardId, user);
        return DashboardResponse.from(dashboard, user.getId(),
                canEdit(dashboard, user, isOrgAdmin(dashboard.getTeam(), user)));
    }

    // ─── Oluşturma / güncelleme / silme ───────────────────────────────────────

    @Transactional
    public DashboardResponse create(UUID teamId, DashboardRequest request) {
        User user = currentUser();
        Team team = requireTeamMembership(teamId, user);

        if (dashboardRepository.countByTeamIdAndOwnerId(teamId, user.getId()) >= MAX_DASHBOARDS_PER_USER) {
            throw new IllegalArgumentException(
                    "Bir takımda en fazla " + MAX_DASHBOARDS_PER_USER + " pano oluşturabilirsiniz.");
        }

        Dashboard dashboard = Dashboard.builder()
                .owner(user)
                .team(team)
                .name(requireUniqueName(teamId, user, request.getName(), null))
                .visibility(request.getVisibility() != null
                        ? request.getVisibility() : DashboardVisibility.PRIVATE)
                .layout(sanitize(request.getLayout(), teamId))
                .position(nextPosition(teamId, user))
                .build();

        Dashboard saved = dashboardRepository.save(dashboard);
        log.info("Pano oluşturuldu: {} (team={}, owner={})", saved.getName(), teamId, user.getEmail());
        return DashboardResponse.from(saved, user.getId(), true);
    }

    /**
     * Ad/görünürlük/sıra ve — gönderilmişse — düzen güncellenir.
     * Düzenin null gelmesi "boşalt" değil "dokunma" demektir (bkz. DashboardRequest).
     */
    @Transactional
    public DashboardResponse update(UUID dashboardId, DashboardRequest request) {
        User user = currentUser();
        Dashboard dashboard = requireEditable(dashboardId, user);

        if (request.getName() != null) {
            dashboard.setName(requireUniqueName(dashboard.getTeam().getId(),
                    dashboard.getOwner(), request.getName(), dashboard));
        }
        if (request.getVisibility() != null) {
            dashboard.setVisibility(request.getVisibility());
        }
        if (request.getPosition() != null) {
            dashboard.setPosition(Math.max(0, request.getPosition()));
        }
        if (request.getLayout() != null) {
            dashboard.setLayout(sanitize(request.getLayout(), dashboard.getTeam().getId()));
        }

        Dashboard saved = dashboardRepository.save(dashboard);
        return DashboardResponse.from(saved, user.getId(), true);
    }

    /** Yalnız düzen — sürükle/boyutlandır sonrası sık çağrılan yol. */
    @Transactional
    public DashboardResponse saveLayout(UUID dashboardId, List<Map<String, Object>> layout) {
        User user = currentUser();
        Dashboard dashboard = requireEditable(dashboardId, user);
        dashboard.setLayout(sanitize(layout, dashboard.getTeam().getId()));
        return DashboardResponse.from(dashboardRepository.save(dashboard), user.getId(), true);
    }

    /**
     * Panoyu kendi adına kopyalar.
     *
     * Takıma açılmış bir panoyu beğenen üyenin tek seçeneği onu olduğu gibi
     * izlemek ya da sıfırdan kurmak olmasın diye var: kopya kendi sahibinindir,
     * özgürce değiştirilir, aslını etkilemez.
     */
    @Transactional
    public DashboardResponse duplicate(UUID dashboardId, String name) {
        User user = currentUser();
        Dashboard source = requireVisible(dashboardId, user);
        UUID teamId = source.getTeam().getId();

        if (dashboardRepository.countByTeamIdAndOwnerId(teamId, user.getId()) >= MAX_DASHBOARDS_PER_USER) {
            throw new IllegalArgumentException(
                    "Bir takımda en fazla " + MAX_DASHBOARDS_PER_USER + " pano oluşturabilirsiniz.");
        }

        String requested = (name == null || name.isBlank()) ? source.getName() + " kopyası" : name;

        Dashboard copy = Dashboard.builder()
                .owner(user)
                .team(source.getTeam())
                .name(requireUniqueName(teamId, user, requested, null))
                .visibility(DashboardVisibility.PRIVATE)
                .layout(copyLayout(source.getLayout()))
                .position(nextPosition(teamId, user))
                .build();

        return DashboardResponse.from(dashboardRepository.save(copy), user.getId(), true);
    }

    @Transactional
    public void delete(UUID dashboardId) {
        User user = currentUser();
        Dashboard dashboard = requireEditable(dashboardId, user);
        dashboardRepository.delete(dashboard);
        log.info("Pano silindi: {} (owner={}, silen={})",
                dashboard.getName(), dashboard.getOwner().getEmail(), user.getEmail());
    }

    // ─── Düzen ────────────────────────────────────────────────────────────────

    /**
     * Düzen JSON'u sunucuda yorumlanmaz ama sınırsız da kabul edilmez: her widget
     * bir kimlik ve tip taşımalı, dizi makul bir boyu aşmamalıdır. Widget'a özel
     * alanlar (eksen, eşik, ölçü…) olduğu gibi geçer — şema arayüzün elindedir.
     */
    private List<Map<String, Object>> sanitize(List<Map<String, Object>> layout, UUID teamId) {
        if (layout == null) return new ArrayList<>();
        if (layout.size() > 40) {
            throw new IllegalArgumentException("Bir panoda en fazla 40 widget olabilir.");
        }

        List<Map<String, Object>> out = new ArrayList<>(layout.size());
        for (Map<String, Object> widget : layout) {
            if (widget == null) continue;
            Object id = widget.get("id");
            Object type = widget.get("type");
            if (id == null || type == null) {
                throw new IllegalArgumentException("Widget kimliği ve tipi zorunlu.");
            }
            Map<String, Object> copy = new LinkedHashMap<>(widget);
            // Widget kendi takımını taşır: pano takıma bağlı olsa da eski düzenlerde
            // alan boş kalabiliyor ve arayüz her seferinde aktif takıma düşürüyordu.
            copy.putIfAbsent("teamId", teamId.toString());
            out.add(copy);
        }
        return out;
    }

    private List<Map<String, Object>> copyLayout(List<Map<String, Object>> layout) {
        if (layout == null) return new ArrayList<>();
        List<Map<String, Object>> out = new ArrayList<>(layout.size());
        for (Map<String, Object> widget : layout) {
            if (widget != null) out.add(new LinkedHashMap<>(widget));
        }
        return out;
    }

    /**
     * İlk açılış panosu. Widget'sız boş bir pano, "widget ekle"nin nerede olduğunu
     * bilmeyen kullanıcıya boş ekran gösterirdi; eski davranışla aynı dört rapor kurulur.
     */
    private Dashboard createDefault(Team team, User user) {
        List<Map<String, Object>> layout = new ArrayList<>();
        layout.add(defaultWidget("default-summary", "SUMMARY", team.getId(), 4));
        layout.add(defaultWidget("default-burndown", "BURNDOWN", team.getId(), 8));
        layout.add(defaultWidget("default-velocity", "VELOCITY", team.getId(), 6));
        layout.add(defaultWidget("default-overdue", "OVERDUE", team.getId(), 6));

        Dashboard dashboard = Dashboard.builder()
                .owner(user)
                .team(team)
                .name(uniqueDefaultName(team.getId(), user))
                .visibility(DashboardVisibility.PRIVATE)
                .layout(layout)
                .position(0)
                .build();
        return dashboardRepository.save(dashboard);
    }

    private Map<String, Object> defaultWidget(String id, String type, UUID teamId, int width) {
        Map<String, Object> widget = new HashMap<>();
        widget.put("id", id);
        widget.put("type", type);
        widget.put("teamId", teamId.toString());
        widget.put("w", width);
        return widget;
    }

    /**
     * Varsayılan pano adı çakışırsa numaralanır: kullanıcı "Panom" adında bir pano
     * kurup hepsini silmişse, kurulum benzersizlik hatasıyla düşmesin.
     */
    private String uniqueDefaultName(UUID teamId, User user) {
        String candidate = DEFAULT_NAME;
        int suffix = 2;
        while (dashboardRepository.existsByTeamIdAndOwnerIdAndNameIgnoreCase(teamId, user.getId(), candidate)) {
            candidate = DEFAULT_NAME + " " + suffix++;
        }
        return candidate;
    }

    private int nextPosition(UUID teamId, User user) {
        return (int) dashboardRepository.countByTeamIdAndOwnerId(teamId, user.getId());
    }

    // ─── Erişim ───────────────────────────────────────────────────────────────

    private Dashboard requireVisible(UUID dashboardId, User user) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new NoSuchElementException("Pano bulunamadı: " + dashboardId));

        boolean owned = dashboard.getOwner().getId().equals(user.getId());
        if (owned) return dashboard;

        boolean shared = dashboard.getVisibility() == DashboardVisibility.TEAM
                && teamMemberRepository.existsByTeamIdAndEmail(dashboard.getTeam().getId(), user.getEmail());
        if (!shared) {
            throw new SecurityException("Bu panoya erişim yetkiniz yok.");
        }
        return dashboard;
    }

    private Dashboard requireEditable(UUID dashboardId, User user) {
        Dashboard dashboard = requireVisible(dashboardId, user);
        if (!canEdit(dashboard, user, isOrgAdmin(dashboard.getTeam(), user))) {
            throw new SecurityException("Bu panoyu düzenleme yetkiniz yok.");
        }
        return dashboard;
    }

    /**
     * Sahibi her zaman düzenler. Organizasyon yöneticisi yalnız TAKIMA AÇILMIŞ
     * panoları düzenleyebilir — yöneticinin başkasının özel panosuna erişmesi,
     * "özel" sözünü anlamsız kılardı.
     */
    private boolean canEdit(Dashboard dashboard, User user, boolean orgAdmin) {
        if (dashboard.getOwner().getId().equals(user.getId())) return true;
        return orgAdmin && dashboard.getVisibility() == DashboardVisibility.TEAM;
    }

    private boolean isOrgAdmin(Team team, User user) {
        if (team.getOrganization() == null) return false;
        return organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(
                team.getOrganization().getId(), user.getEmail(), ADMIN_ROLES);
    }

    private Team requireTeamMembership(UUID teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Takım bulunamadı: " + teamId));
        if (!teamMemberRepository.existsByTeamIdAndEmail(teamId, user.getEmail())) {
            throw new SecurityException("Bu takımın üyesi değilsiniz.");
        }
        return team;
    }

    /**
     * Ad, kişinin kendi panoları içinde benzersizdir — takım genelinde değil.
     * İki kişinin ayrı ayrı "Sürüm takibi" adında panosu olması doğaldır; sekmede
     * paylaşılanın yanında sahibinin adı yazar.
     */
    private String requireUniqueName(UUID teamId, User owner, String name, Dashboard current) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Pano adı boş olamaz.");
        }
        if (trimmed.length() > 60) {
            throw new IllegalArgumentException("Pano adı en fazla 60 karakter olabilir.");
        }
        boolean unchanged = current != null
                && current.getName().toLowerCase(Locale.ROOT).equals(trimmed.toLowerCase(Locale.ROOT));
        if (!unchanged
                && dashboardRepository.existsByTeamIdAndOwnerIdAndNameIgnoreCase(teamId, owner.getId(), trimmed)) {
            throw new IllegalArgumentException("Bu adda bir panonuz zaten var: " + trimmed);
        }
        return trimmed;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new SecurityException("Oturum bulunamadı.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı: " + auth.getName()));
    }
}

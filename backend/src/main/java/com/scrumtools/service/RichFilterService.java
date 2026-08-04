package com.scrumtools.service;

import com.scrumtools.dto.RichFilterElementRequest;
import com.scrumtools.dto.RichFilterElementResponse;
import com.scrumtools.dto.RichFilterRequest;
import com.scrumtools.dto.RichFilterResponse;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.FilterVisibility;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.RichFilterElementKind;
import com.scrumtools.query.QueryParser;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Zengin filtre yönetimi — tanım tarafı (CRUD).
 *
 * Görünürlük kuralı {@link SavedFilterService} ile aynıdır (PRIVATE / TEAM / PROJECT)
 * ve düzenleme her durumda yalnız sahibine açıktır: paylaşılan bir zengin filtreyi
 * başkasının altından değiştirmek, ona bağlı bütün dashboard'ları sessizce bozardı.
 *
 * Paket denetimi yalnız yazma yollarındadır; okuma ve çalıştırma her pakette
 * serbesttir (bkz. RICH_FILTER_PLAN.md — K16).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RichFilterService {

    /** Takım başına azami zengin filtre — kötüye kullanıma karşı uygulama tavanı. */
    private static final int MAX_FILTERS_PER_TEAM = 25;
    /** Zengin filtre başına azami öğe. */
    private static final int MAX_ELEMENTS = 50;

    /**
     * Renk verilmediğinde sıraya göre atanan palet.
     * Ekrandaki Jira örneğiyle aynı mantık: her akıllı filtre ayırt edilebilir bir
     * renkle gelsin, kullanıcı isterse değiştirsin.
     */
    private static final List<String> DEFAULT_COLORS = List.of(
            "#6366F1", "#0EA5E9", "#10B981", "#F59E0B",
            "#EF4444", "#8B5CF6", "#EC4899", "#64748B");

    private final RichFilterRepository richFilterRepository;
    private final RichFilterElementRepository elementRepository;
    private final RichFilterSeriesPointRepository seriesPointRepository;
    private final SavedFilterRepository savedFilterRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final EntitlementService entitlementService;

    // ─── Listeleme ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RichFilterResponse> list(UUID teamId, UUID projectId) {
        User user = currentUser();
        requireTeamMembership(teamId, user);

        LinkedHashMap<UUID, RichFilter> merged = new LinkedHashMap<>();
        richFilterRepository.findVisibleForUser(teamId, user.getEmail())
                .forEach(r -> merged.put(r.getId(), r));

        if (projectId != null) {
            richFilterRepository.findProjectSharedOutsideTeam(projectId, teamId)
                    .forEach(r -> merged.putIfAbsent(r.getId(), r));
        }

        return merged.values().stream()
                .map(r -> RichFilterResponse.from(r, user.getId()))
                .sorted(Comparator.comparing(r -> r.name().toLowerCase(Locale.ROOT)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RichFilterResponse get(UUID richFilterId) {
        User user = currentUser();
        return RichFilterResponse.from(requireAccessible(richFilterId, user), user.getId());
    }

    // ─── Oluşturma / güncelleme / silme ───────────────────────────────────────

    @Transactional
    public RichFilterResponse create(UUID teamId, RichFilterRequest request) {
        User user = currentUser();
        Team team = requireTeamMembership(teamId, user);
        entitlementService.assertFeature(team.getOrganization(), PlanFeature.RICH_FILTERS);

        if (richFilterRepository.countByTeamId(teamId) >= MAX_FILTERS_PER_TEAM) {
            throw new IllegalArgumentException(
                    "Bir takımda en fazla " + MAX_FILTERS_PER_TEAM + " zengin filtre olabilir.");
        }
        String name = requireUniqueName(teamId, request.getName(), null);

        FilterVisibility visibility = request.getVisibility() != null
                ? request.getVisibility() : FilterVisibility.PRIVATE;

        RichFilter filter = RichFilter.builder()
                .owner(user)
                .team(team)
                .project(resolveProject(request.getProjectId(), visibility))
                .name(name)
                .description(request.getDescription())
                .baseFilter(resolveBaseFilter(request.getBaseFilterId(), user))
                .baseQuery(validatedQuery(request.getBaseQuery()))
                .visibility(visibility)
                .build();

        RichFilter saved = richFilterRepository.save(filter);
        log.info("Zengin filtre oluşturuldu: {} (team={}, owner={})", saved.getName(), teamId, user.getEmail());
        return RichFilterResponse.from(saved, user.getId());
    }

    @Transactional
    public RichFilterResponse update(UUID richFilterId, RichFilterRequest request) {
        User user = currentUser();
        RichFilter filter = requireOwned(richFilterId, user);
        entitlementService.assertFeature(filter.getTeam().getOrganization(), PlanFeature.RICH_FILTERS);

        FilterVisibility visibility = request.getVisibility() != null
                ? request.getVisibility() : filter.getVisibility();

        filter.setName(requireUniqueName(filter.getTeam().getId(), request.getName(), filter));
        filter.setDescription(request.getDescription());
        filter.setBaseFilter(resolveBaseFilter(request.getBaseFilterId(), user));
        filter.setBaseQuery(validatedQuery(request.getBaseQuery()));
        filter.setVisibility(visibility);
        filter.setProject(resolveProject(request.getProjectId(), visibility));

        return RichFilterResponse.from(richFilterRepository.save(filter), user.getId());
    }

    @Transactional
    public void delete(UUID richFilterId) {
        User user = currentUser();
        RichFilter filter = requireOwned(richFilterId, user);
        // Zaman serisi noktaları öğelere dışarıdan bağlı; cascade onları kapsamaz.
        seriesPointRepository.deleteByElementRichFilterId(richFilterId);
        richFilterRepository.delete(filter);
    }

    /**
     * Zengin filtreyi öğeleriyle birlikte kopyalar.
     * Kopya her zaman kopyalayanın kendi PRIVATE kaydıdır: paylaşılan bir filtreden
     * türetilen kopyanın da paylaşılmış olması beklenmez.
     */
    @Transactional
    public RichFilterResponse duplicate(UUID richFilterId) {
        User user = currentUser();
        RichFilter source = requireAccessible(richFilterId, user);
        entitlementService.assertFeature(source.getTeam().getOrganization(), PlanFeature.RICH_FILTERS);

        RichFilter copy = RichFilter.builder()
                .owner(user)
                .team(source.getTeam())
                .project(source.getProject())
                .name(uniqueCopyName(source.getTeam().getId(), source.getName()))
                .description(source.getDescription())
                .baseFilter(source.getBaseFilter())
                .baseQuery(source.getBaseQuery())
                .visibility(FilterVisibility.PRIVATE)
                .build();

        for (RichFilterElement element : source.getElements()) {
            copy.getElements().add(RichFilterElement.builder()
                    .richFilter(copy)
                    .kind(element.getKind())
                    .name(element.getName())
                    .query(element.getQuery())
                    .color(element.getColor())
                    .position(element.getPosition())
                    .config(new LinkedHashMap<>(element.configOrEmpty()))
                    .build());
        }

        return RichFilterResponse.from(richFilterRepository.save(copy), user.getId());
    }

    // ─── Öğeler ───────────────────────────────────────────────────────────────

    @Transactional
    public RichFilterElementResponse addElement(UUID richFilterId, RichFilterElementRequest request) {
        User user = currentUser();
        RichFilter filter = requireOwned(richFilterId, user);
        entitlementService.assertFeature(filter.getTeam().getOrganization(), PlanFeature.RICH_FILTERS);

        if (elementRepository.countByRichFilterId(richFilterId) >= MAX_ELEMENTS) {
            throw new IllegalArgumentException(
                    "Bir zengin filtrede en fazla " + MAX_ELEMENTS + " öğe olabilir.");
        }
        validateElement(filter, request, null);

        int position = filter.getElements().stream()
                .filter(e -> e.getKind() == request.getKind())
                .mapToInt(e -> e.getPosition() == null ? 0 : e.getPosition())
                .max().orElse(-1) + 1;

        RichFilterElement element = RichFilterElement.builder()
                .richFilter(filter)
                .kind(request.getKind())
                .name(request.getName().trim())
                .query(validatedQuery(request.getQuery()))
                .color(resolveColor(request.getColor(), position))
                .position(position)
                .config(request.getConfig() != null ? new LinkedHashMap<>(request.getConfig()) : new LinkedHashMap<>())
                .build();

        RichFilterElement saved = elementRepository.save(element);
        // Nesne grafiği tutarlı kalsın: aynı işlem içinde ikinci bir öğe eklenirse
        // ad benzersizliği denetimi yenisini de görmeli.
        filter.getElements().add(saved);
        return RichFilterElementResponse.from(saved);
    }

    @Transactional
    public RichFilterElementResponse updateElement(UUID richFilterId, UUID elementId,
                                                   RichFilterElementRequest request) {
        User user = currentUser();
        RichFilter filter = requireOwned(richFilterId, user);
        entitlementService.assertFeature(filter.getTeam().getOrganization(), PlanFeature.RICH_FILTERS);

        RichFilterElement element = requireElement(filter, elementId);
        validateElement(filter, request, element);

        // Tür değiştirilemez: config şeması türe bağlıdır, tür değişince eski
        // yapılandırma sessizce anlamsızlaşırdı.
        element.setName(request.getName().trim());
        element.setQuery(validatedQuery(request.getQuery()));
        element.setColor(resolveColor(request.getColor(), element.getPosition()));
        if (request.getConfig() != null) {
            element.setConfig(new LinkedHashMap<>(request.getConfig()));
        }

        return RichFilterElementResponse.from(elementRepository.save(element));
    }

    @Transactional
    public void deleteElement(UUID richFilterId, UUID elementId) {
        User user = currentUser();
        RichFilter filter = requireOwned(richFilterId, user);

        RichFilterElement element = requireElement(filter, elementId);
        seriesPointRepository.deleteByElementId(elementId);
        filter.getElements().remove(element);
        elementRepository.delete(element);
    }

    /**
     * Aynı türdeki öğelerin sırasını değiştirir.
     *
     * Akıllı filtrelerde sıra sonucu belirler: bir görev, kendisine uyan ilk
     * akıllı filtrenin rengini ve etiketini alır (bkz. RICH_FILTER_PLAN.md K4).
     */
    @Transactional
    public List<RichFilterElementResponse> reorderElements(UUID richFilterId, List<UUID> orderedIds) {
        User user = currentUser();
        RichFilter filter = requireOwned(richFilterId, user);

        Map<UUID, RichFilterElement> byId = new LinkedHashMap<>();
        filter.getElements().forEach(e -> byId.put(e.getId(), e));

        int position = 0;
        for (UUID id : orderedIds) {
            RichFilterElement element = byId.get(id);
            if (element == null) {
                throw new NoSuchElementException("Öğe bu zengin filtreye ait değil: " + id);
            }
            element.setPosition(position++);
        }
        elementRepository.saveAll(byId.values());

        return filter.getElements().stream()
                .sorted(Comparator.comparingInt(e -> e.getPosition() == null ? 0 : e.getPosition()))
                .map(RichFilterElementResponse::from)
                .toList();
    }

    // ─── Doğrulama ────────────────────────────────────────────────────────────

    private void validateElement(RichFilter filter, RichFilterElementRequest request,
                                 RichFilterElement existing) {
        RichFilterElementKind kind = existing != null ? existing.getKind() : request.getKind();
        String name = request.getName() == null ? "" : request.getName().trim();

        // Akıllı filtre adı STQL'de değer olarak yazılır (smart["RF"] = "Test");
        // aynı ad iki kez kullanılırsa o sorgu hangisini kastettiğini bilemez.
        boolean duplicate = filter.getElements().stream()
                .filter(e -> e.getKind() == kind)
                .filter(e -> existing == null || !e.getId().equals(existing.getId()))
                .anyMatch(e -> e.getName() != null && e.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new IllegalArgumentException("Bu zengin filtrede '" + name + "' adlı bir öğe zaten var.");
        }

        if (kind == RichFilterElementKind.SMART_FILTER
                && (request.getQuery() == null || request.getQuery().isBlank())) {
            throw new IllegalArgumentException("Akıllı filtrenin sorgusu boş olamaz.");
        }
        if (kind == RichFilterElementKind.TIME_SERIES) {
            validateSeriesTarget(filter, request);
        }
        if (request.getColor() != null && !request.getColor().isBlank()
                && !request.getColor().matches("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")) {
            throw new IllegalArgumentException("Renk #RRGGBB biçiminde olmalıdır.");
        }
    }

    /**
     * Zaman serisi bir akıllı filtreye bağlanır (boş bırakılırsa tüm sonuçlar).
     *
     * Bağ kaydedilirken doğrulanır: var olmayan bir id, geçmişi kurgulanamayan ve
     * gecelik iş her gece sessizce hata veren bir seri bırakırdı.
     */
    private void validateSeriesTarget(RichFilter filter, RichFilterElementRequest request) {
        Object raw = request.getConfig() == null ? null : request.getConfig().get("smartFilterId");
        if (raw == null || raw.toString().isBlank()) return;

        UUID smartId;
        try {
            smartId = UUID.fromString(raw.toString().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Zaman serisinin bağlandığı akıllı filtre geçersiz.");
        }

        boolean exists = filter.getElements().stream()
                .anyMatch(e -> e.getKind() == RichFilterElementKind.SMART_FILTER && e.getId().equals(smartId));
        if (!exists) {
            throw new IllegalArgumentException(
                    "Zaman serisi, bu zengin filtredeki bir akıllı filtreye bağlanmalıdır.");
        }
    }

    /** Sorgu kaydedilmeden önce çözümlenir — bozuk sorgu kaydedilip sonra patlamasın. */
    private String validatedQuery(String query) {
        if (query == null || query.isBlank()) return null;
        QueryParser.parse(query);
        return query.trim();
    }

    private String requireUniqueName(UUID teamId, String rawName, RichFilter existing) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Zengin filtre adı zorunludur.");
        }
        boolean unchanged = existing != null && existing.getName().equalsIgnoreCase(name);
        if (!unchanged && richFilterRepository.existsByTeamIdAndNameIgnoreCase(teamId, name)) {
            throw new IllegalArgumentException(
                    "Bu takımda '" + name + "' adlı bir zengin filtre zaten var. "
                            + "Adlar benzersiz olmalı: STQL'de smart[\"" + name + "\"] yazımı filtreyi adıyla çözer.");
        }
        return name;
    }

    private String uniqueCopyName(UUID teamId, String baseName) {
        String candidate = baseName + " (kopya)";
        int suffix = 2;
        while (richFilterRepository.existsByTeamIdAndNameIgnoreCase(teamId, candidate)) {
            candidate = baseName + " (kopya " + suffix++ + ")";
        }
        return candidate;
    }

    private String resolveColor(String requested, Integer position) {
        if (requested != null && !requested.isBlank()) return requested.trim();
        int index = position == null ? 0 : Math.abs(position);
        return DEFAULT_COLORS.get(index % DEFAULT_COLORS.size());
    }

    // ─── Yetki ────────────────────────────────────────────────────────────────

    /** Okuma yetkisi — görünürlük kuralına göre. Diğer fazlar da bunu kullanır. */
    @Transactional(readOnly = true)
    public RichFilter requireAccessible(UUID richFilterId, User user) {
        RichFilter filter = richFilterRepository.findById(richFilterId)
                .orElseThrow(() -> new NoSuchElementException("Zengin filtre bulunamadı: " + richFilterId));

        if (filter.getOwner().getId().equals(user.getId())) return filter;

        boolean allowed = switch (filter.getVisibility()) {
            case PRIVATE -> false;
            case TEAM -> teamMemberRepository.existsByTeamIdAndEmail(filter.getTeam().getId(), user.getEmail());
            case PROJECT -> filter.effectiveProject() != null
                    && isProjectMember(filter.effectiveProject().getId(), user);
        };
        if (!allowed) {
            throw new SecurityException("Bu zengin filtreye erişim yetkiniz yok.");
        }
        return filter;
    }

    private RichFilter requireOwned(UUID richFilterId, User user) {
        RichFilter filter = richFilterRepository.findById(richFilterId)
                .orElseThrow(() -> new NoSuchElementException("Zengin filtre bulunamadı: " + richFilterId));
        if (!filter.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Bu zengin filtreyi yalnızca oluşturan kişi değiştirebilir.");
        }
        return filter;
    }

    private RichFilterElement requireElement(RichFilter filter, UUID elementId) {
        return filter.getElements().stream()
                .filter(e -> e.getId().equals(elementId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Öğe bulunamadı: " + elementId));
    }

    private boolean isProjectMember(UUID projectId, User user) {
        return teamRepository.findByProjectId(projectId).stream()
                .anyMatch(t -> teamMemberRepository.existsByTeamIdAndEmail(t.getId(), user.getEmail()));
    }

    private Team requireTeamMembership(UUID teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Takım bulunamadı: " + teamId));
        if (!teamMemberRepository.existsByTeamIdAndEmail(teamId, user.getEmail())) {
            throw new SecurityException("Bu takımın üyesi değilsiniz.");
        }
        return team;
    }

    // ─── Bağımlı kayıtlar ─────────────────────────────────────────────────────

    /** Temel sorgu kayıtlı filtreye bağlanacaksa kullanıcının ona erişimi olmalı. */
    private SavedFilter resolveBaseFilter(UUID baseFilterId, User user) {
        if (baseFilterId == null) return null;

        SavedFilter base = savedFilterRepository.findById(baseFilterId)
                .orElseThrow(() -> new NoSuchElementException("Kayıtlı filtre bulunamadı: " + baseFilterId));

        boolean allowed = base.getOwner().getId().equals(user.getId())
                || switch (base.getVisibility()) {
            case PRIVATE -> false;
            case TEAM -> teamMemberRepository.existsByTeamIdAndEmail(base.getTeam().getId(), user.getEmail());
            case PROJECT -> base.getProject() != null && isProjectMember(base.getProject().getId(), user);
        };
        if (!allowed) {
            throw new SecurityException("Temel filtreye erişim yetkiniz yok.");
        }
        return base;
    }

    private Project resolveProject(UUID projectId, FilterVisibility visibility) {
        if (projectId == null) {
            if (visibility == FilterVisibility.PROJECT) {
                throw new IllegalArgumentException("Proje geneline açılan filtrelerde proje seçilmelidir.");
            }
            return null;
        }
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Proje bulunamadı: " + projectId));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new SecurityException("Oturum bulunamadı.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı: " + auth.getName()));
    }

    /** Oturumdaki kullanıcı — diğer fazların servisleri için. */
    @Transactional(readOnly = true)
    public User currentUserOrThrow() {
        return currentUser();
    }
}

package com.scrumtools.service;

import com.scrumtools.dto.SavedFilterRequest;
import com.scrumtools.dto.SavedFilterResponse;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.SavedFilter;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.FilterVisibility;
import com.scrumtools.query.QueryParser;
import com.scrumtools.query.TaskQueryService;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Kayıtlı filtre yönetimi.
 *
 * Görünürlük kuralı:
 *  - PRIVATE: yalnız sahibi
 *  - TEAM:    filtrenin takımının üyeleri
 *  - PROJECT: filtrenin projesinde çalışan tüm takımların üyeleri
 *
 * Düzenleme ve silme her durumda yalnız sahibine açıktır — paylaşılan bir filtreyi
 * başkasının altından değiştirmek, o filtreye bağlı görünümleri sessizce bozardı.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {

    private final SavedFilterRepository savedFilterRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskQueryService taskQueryService;

    // ─── Listeleme ────────────────────────────────────────────────────────────

    /**
     * Kullanıcının bir takım bağlamında görebildiği filtreler.
     * Aktif proje verilmişse o projeye açılmış, başka takımlardan gelen filtreler de eklenir.
     */
    @Transactional(readOnly = true)
    public List<SavedFilterResponse> list(UUID teamId, UUID projectId) {
        User user = currentUser();
        requireTeamMembership(teamId, user);

        LinkedHashMap<UUID, SavedFilter> merged = new LinkedHashMap<>();
        savedFilterRepository.findVisibleForUser(teamId, user.getEmail())
                .forEach(f -> merged.put(f.getId(), f));

        if (projectId != null) {
            savedFilterRepository.findProjectSharedOutsideTeam(projectId, teamId)
                    .forEach(f -> merged.putIfAbsent(f.getId(), f));
        }

        // Favoriler üstte, sonra ada göre.
        return merged.values().stream()
                .map(f -> SavedFilterResponse.from(f, user.getId()))
                .sorted(Comparator.comparing(SavedFilterResponse::favorite).reversed()
                        .thenComparing(r -> r.name().toLowerCase(Locale.ROOT)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SavedFilterResponse get(UUID filterId) {
        User user = currentUser();
        SavedFilter filter = findAccessible(filterId, user);
        return SavedFilterResponse.from(filter, user.getId());
    }

    // ─── Oluşturma / güncelleme / silme ───────────────────────────────────────

    @Transactional
    public SavedFilterResponse create(UUID teamId, SavedFilterRequest request) {
        User user = currentUser();
        Team team = requireTeamMembership(teamId, user);

        validateQuery(request.getQuery());
        FilterVisibility visibility = request.getVisibility() != null
                ? request.getVisibility() : FilterVisibility.PRIVATE;
        Project project = resolveProject(request.getProjectId(), visibility);

        SavedFilter filter = SavedFilter.builder()
                .owner(user)
                .team(team)
                .project(project)
                .name(request.getName().trim())
                .description(request.getDescription())
                .query(request.getQuery() == null ? "" : request.getQuery().trim())
                .visibility(visibility)
                .build();

        SavedFilter saved = savedFilterRepository.save(filter);
        log.info("Kayıtlı filtre oluşturuldu: {} (team={}, owner={})",
                saved.getName(), teamId, user.getEmail());
        return SavedFilterResponse.from(saved, user.getId());
    }

    @Transactional
    public SavedFilterResponse update(UUID filterId, SavedFilterRequest request) {
        User user = currentUser();
        SavedFilter filter = savedFilterRepository.findById(filterId)
                .orElseThrow(() -> new NoSuchElementException("Filtre bulunamadı: " + filterId));
        requireOwnership(filter, user);

        validateQuery(request.getQuery());
        FilterVisibility visibility = request.getVisibility() != null
                ? request.getVisibility() : filter.getVisibility();

        filter.setName(request.getName().trim());
        filter.setDescription(request.getDescription());
        filter.setQuery(request.getQuery() == null ? "" : request.getQuery().trim());
        filter.setVisibility(visibility);
        filter.setProject(resolveProject(request.getProjectId(), visibility));

        return SavedFilterResponse.from(savedFilterRepository.save(filter), user.getId());
    }

    @Transactional
    public void delete(UUID filterId) {
        User user = currentUser();
        SavedFilter filter = savedFilterRepository.findById(filterId)
                .orElseThrow(() -> new NoSuchElementException("Filtre bulunamadı: " + filterId));
        requireOwnership(filter, user);
        savedFilterRepository.delete(filter);
    }

    // ─── Favori ───────────────────────────────────────────────────────────────

    /** Yıldızlar veya yıldızı kaldırır; sonuçtaki favorite alanı yeni durumu yansıtır. */
    @Transactional
    public SavedFilterResponse toggleFavorite(UUID filterId, boolean favorite) {
        User user = currentUser();
        SavedFilter filter = findAccessible(filterId, user);

        if (favorite) filter.getFavoritedBy().add(user.getId());
        else filter.getFavoritedBy().remove(user.getId());

        return SavedFilterResponse.from(savedFilterRepository.save(filter), user.getId());
    }

    // ─── Çalıştırma ───────────────────────────────────────────────────────────

    /**
     * Kayıtlı filtreyi çalıştırır.
     * Proje kapsamı istekle gelmezse filtrenin kendi projesi kullanılır.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> run(UUID filterId, UUID projectId, Integer page, Integer size) {
        User user = currentUser();
        SavedFilter filter = findAccessible(filterId, user);

        UUID scope = projectId != null
                ? projectId
                : (filter.getProject() != null ? filter.getProject().getId() : null);

        return taskQueryService.search(filter.getTeam().getId(), scope, filter.getQuery(), page, size);
    }

    // ─── Yetki ve doğrulama ───────────────────────────────────────────────────

    /** Kullanıcının filtreyi görmeye hakkı var mı — yoksa erişim hatası fırlatır. */
    private SavedFilter findAccessible(UUID filterId, User user) {
        SavedFilter filter = savedFilterRepository.findById(filterId)
                .orElseThrow(() -> new NoSuchElementException("Filtre bulunamadı: " + filterId));

        if (filter.getOwner().getId().equals(user.getId())) return filter;

        boolean allowed = switch (filter.getVisibility()) {
            case PRIVATE -> false;
            case TEAM -> teamMemberRepository.existsByTeamIdAndEmail(filter.getTeam().getId(), user.getEmail());
            case PROJECT -> filter.getProject() != null && isProjectMember(filter.getProject().getId(), user);
        };
        if (!allowed) {
            throw new SecurityException("Bu filtreye erişim yetkiniz yok.");
        }
        return filter;
    }

    /** Kullanıcı, projede çalışan takımlardan herhangi birinin üyesi mi? */
    private boolean isProjectMember(UUID projectId, User user) {
        return teamRepository.findByProjectId(projectId).stream()
                .anyMatch(t -> teamMemberRepository.existsByTeamIdAndEmail(t.getId(), user.getEmail()));
    }

    private void requireOwnership(SavedFilter filter, User user) {
        if (!filter.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Bu filtreyi yalnızca oluşturan kişi değiştirebilir.");
        }
    }

    private Team requireTeamMembership(UUID teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Takım bulunamadı: " + teamId));
        if (!teamMemberRepository.existsByTeamIdAndEmail(teamId, user.getEmail())) {
            throw new SecurityException("Bu takımın üyesi değilsiniz.");
        }
        return team;
    }

    private Project resolveProject(UUID projectId, FilterVisibility visibility) {
        if (projectId == null) {
            if (visibility == FilterVisibility.PROJECT) {
                throw new IllegalArgumentException(
                        "Proje geneline açılan filtrelerde proje seçilmelidir.");
            }
            return null;
        }
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Proje bulunamadı: " + projectId));
    }

    /** Sorgu kaydedilmeden önce çözümlenir — bozuk sorgu kaydedilip sonra patlamasın. */
    private void validateQuery(String query) {
        QueryParser.parse(query);
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

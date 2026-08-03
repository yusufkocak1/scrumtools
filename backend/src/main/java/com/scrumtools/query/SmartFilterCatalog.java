package com.scrumtools.query;

import com.scrumtools.entity.RichFilter;
import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.enums.RichFilterElementKind;
import com.scrumtools.repository.RichFilterRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * {@code smart["zengin filtre"]} yazımının çözüm kaynağı — sorgu motoruyla zengin
 * filtre kayıtları arasındaki köprü.
 *
 * Sorgu paketi repository'lere doğrudan bağlanmasın diye {@link QueryContext} bu
 * arayüz üzerinden besleniyor; {@code currentSprint()} fonksiyonunun sprint
 * çözücüsüyle aynı desen.
 *
 * Bkz. RICH_FILTER_PLAN.md — K18.
 */
@Service
@RequiredArgsConstructor
public class SmartFilterCatalog implements QueryContext.SmartFilterResolver {

    private final RichFilterRepository richFilterRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    /**
     * Zengin filtrenin sıralı akıllı filtreleri.
     *
     * Kullanıcı zengin filtreyi göremiyorsa, kayıt hiç yokmuş gibi {@code null} döner:
     * "var ama senin değil" bilgisi tek başına kimin hangi sınıflandırmayı kurduğunu
     * sızdırırdı. Sonuçların kendisi zaten takım kapsamıyla sınırlıdır.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SmartClause> clausesOf(UUID teamId, String richFilterName) {
        if (teamId == null || richFilterName == null || richFilterName.isBlank()) return null;

        RichFilter filter = richFilterRepository
                .findByTeamAndNameWithElements(teamId, richFilterName.trim())
                .orElse(null);
        if (filter == null || !isAccessible(filter)) return null;

        return filter.getElements().stream()
                .filter(e -> e.getKind() == RichFilterElementKind.SMART_FILTER)
                .sorted(Comparator.comparingInt(e -> e.getPosition() == null ? 0 : e.getPosition()))
                .map(e -> toClause(filter, e))
                .toList();
    }

    private SmartClause toClause(RichFilter filter, RichFilterElement element) {
        ParsedQuery parsed;
        try {
            parsed = QueryParser.parse(element.getQuery());
        } catch (QueryParseException e) {
            // Kaydederken doğrulanıyor; buraya düşmesi verinin dilden eski kaldığı
            // anlamına gelir — hangi kuralın bozuk olduğunu söylemek şart.
            throw new QueryParseException(
                    "'" + filter.getName() + "' zengin filtresindeki '" + element.getName()
                            + "' akıllı filtresinin sorgusu çözümlenemedi: " + e.getMessage(), 0, 1);
        }
        return new SmartClause(filter.getId(), element.getId(), element.getName(),
                element.getColor(), parsed);
    }

    private boolean isAccessible(RichFilter filter) {
        String email = currentUserEmail();
        if (email == null) return false;
        if (email.equalsIgnoreCase(filter.getOwner().getEmail())) return true;

        return switch (filter.getVisibility()) {
            case PRIVATE -> false;
            case TEAM -> teamMemberRepository.existsByTeamIdAndEmail(filter.getTeam().getId(), email);
            case PROJECT -> filter.effectiveProject() != null
                    && teamRepository.findByProjectId(filter.effectiveProject().getId()).stream()
                    .anyMatch(t -> teamMemberRepository.existsByTeamIdAndEmail(t.getId(), email));
        };
    }

    private static String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}

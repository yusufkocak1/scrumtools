package com.scrumtools.service;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.RichFilter;
import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.enums.RichFilterElementKind;
import com.scrumtools.query.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Zengin filtrenin sorgularını <b>oturumsuz</b> kuran fabrika.
 *
 * Zamanlanmış işlerin ortak sorunu: {@code SecurityContextHolder} boştur, bu yüzden
 * {@code currentUser()} çözülemez ve görünürlük denetimi yapan {@link SmartFilterCatalog}
 * zengin filtreyi "erişilemez" sayıp {@code null} döner. İki iş (zaman serisi ölçümü ve
 * oran uyarıları) aynı çözümü kullanıyor: bağlam filtrenin <b>sahibi</b> adına kurulur ve
 * akıllı filtreler kayıt aramadan, elimizdeki nesnenin kendi öğelerinden çözülür — kendi
 * öğesine erişim zaten tanımın parçasıdır.
 *
 * İki iş bunu ayrı ayrı yazsaydı, biri düzeltilip diğeri unutulduğunda ölçümle uyarı
 * farklı sayılar üretirdi.
 */
@Component
@RequiredArgsConstructor
public class RichFilterQueryFactory {

    private final TaskQueryService taskQueryService;

    /** Filtrenin kapsam projesi; yoksa takımın tamamı. */
    public UUID scopeOf(RichFilter filter) {
        Project project = filter.effectiveProject();
        return project != null ? project.getId() : null;
    }

    /** Temel sorgu — kayıtlı filtre referansı varsa onunki. */
    public ParsedQuery base(RichFilter filter) {
        return QueryParser.parse(filter.effectiveBaseQuery());
    }

    /**
     * Temel sorgu AND verilen akıllı filtrenin kovası.
     *
     * {@code smart[…]} üzerinden gider, kuralın sorgusunu doğrudan AND'lemez:
     * "ilk eşleşen kazanır" semantiği ancak böyle korunur, yoksa uyarı grafikteki
     * dilimden farklı bir sayı üretirdi.
     */
    public ParsedQuery boundedBy(RichFilter filter, RichFilterElement smartElement) {
        if (smartElement == null) return base(filter);
        return QueryComposer.compose(base(filter), List.of(List.of(
                QueryFragments.smartIn(filter.getName(), List.of(smartElement.getName())))));
    }

    /** Filtrenin kendi akıllı filtreleri, sıra korunarak. */
    public List<SmartClause> clausesOf(RichFilter filter) {
        return smartFilters(filter).stream()
                .map(e -> new SmartClause(filter.getId(), e.getId(), e.getName(), e.getColor(),
                        QueryParser.parse(e.getQuery())))
                .toList();
    }

    public List<RichFilterElement> smartFilters(RichFilter filter) {
        return elementsOf(filter, RichFilterElementKind.SMART_FILTER);
    }

    public static List<RichFilterElement> elementsOf(RichFilter filter, RichFilterElementKind kind) {
        return filter.getElements().stream()
                .filter(e -> e.getKind() == kind)
                .sorted(Comparator.comparingInt(e -> e.getPosition() == null ? 0 : e.getPosition()))
                .toList();
    }

    /** Config'te id ile gösterilen akıllı filtre; silinmişse boş döner. */
    public Optional<RichFilterElement> smartFilterById(RichFilter filter, Object rawId) {
        if (rawId == null || rawId.toString().isBlank()) return Optional.empty();
        UUID id;
        try {
            id = UUID.fromString(rawId.toString().trim());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return smartFilters(filter).stream().filter(e -> e.getId().equals(id)).findFirst();
    }

    /** Filtrenin sahibi adına, kendi akıllı filtrelerini tanıyan bağlam. */
    public QueryContext contextOf(RichFilter filter) {
        List<SmartClause> clauses = clausesOf(filter);
        return taskQueryService.buildContext(
                filter.getTeam().getId(),
                scopeOf(filter),
                filter.getOwner().getEmail(),
                (_, name) -> name != null && name.equalsIgnoreCase(filter.getName()) ? clauses : null);
    }
}

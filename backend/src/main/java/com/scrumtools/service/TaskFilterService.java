package com.scrumtools.service;

import com.scrumtools.dto.TaskFilterRequest;
import com.scrumtools.query.LegacyFilterTranslator;
import com.scrumtools.query.ParsedQuery;
import com.scrumtools.query.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Eski JSON filtre ucunun ({@code POST /api/teams/{teamId}/tasks/filter}) uyarlayıcısı.
 *
 * Kendi sorgu üretimi yoktur; {@code filters[]} listesini {@link LegacyFilterTranslator}
 * ile STQL ağacına çevirip {@link TaskQueryService} üzerinde çalıştırır. Böylece
 * arayüzün "Basit" sekmesi ile "STQL" sekmesi aynı motoru kullanır ve iki filtreleme
 * kod yolunun birbirinden ayrışması engellenir.
 *
 * Not: eski uygulamada {@code labels contains X} tam eşleşme yapıyordu; yeni motorda
 * gerçekten alt dizge araması yapar — arayüzdeki "İçeriyor" etiketiyle uyumlu hâle geldi.
 */
@Service
@RequiredArgsConstructor
public class TaskFilterService {

    private final TaskQueryService taskQueryService;

    @Transactional(readOnly = true)
    public Map<String, Object> filter(UUID teamId, TaskFilterRequest req) {
        ParsedQuery parsed = new ParsedQuery(
                LegacyFilterTranslator.toNode(req.getFilters()),
                List.of());

        return taskQueryService.execute(
                teamId,
                req.getProjectId(),
                parsed,
                req.getSortBy(),
                req.getSortDir(),
                req.getPage(),
                req.getSize());
    }
}

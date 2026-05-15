package com.scrumtools.service;

import com.scrumtools.dto.TaskFilterCriteria;
import com.scrumtools.dto.TaskFilterRequest;
import com.scrumtools.dto.TaskResponse;
import com.scrumtools.entity.Task;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Faz 4 — Dinamik JSON filtre motoru.
 * JPA Criteria API ile field/operator/values üçlüsünden WHERE koşulları üretir.
 */
@Service
@RequiredArgsConstructor
public class TaskFilterService {

    private final EntityManager em;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> filter(UUID teamId, TaskFilterRequest req) {
        // Team kontrolü
        teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // ─── COUNT sorgusu ────────────────────────────────────────────────────
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Task> countRoot = countQuery.from(Task.class);
        countQuery.select(cb.count(countRoot));
        List<Predicate> countPreds = buildPredicates(cb, countRoot, teamId, req.getFilters());
        if (!countPreds.isEmpty()) {
            countQuery.where(cb.and(countPreds.toArray(new Predicate[0])));
        }
        long total = em.createQuery(countQuery).getSingleResult();

        // ─── DATA sorgusu ─────────────────────────────────────────────────────
        CriteriaQuery<Task> dataQuery = cb.createQuery(Task.class);
        Root<Task> root = dataQuery.from(Task.class);
        dataQuery.select(root);

        List<Predicate> preds = buildPredicates(cb, root, teamId, req.getFilters());
        if (!preds.isEmpty()) {
            dataQuery.where(cb.and(preds.toArray(new Predicate[0])));
        }

        // Sıralama
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "createdAt";
        String sortDir = req.getSortDir() != null ? req.getSortDir() : "desc";
        Order order = "asc".equalsIgnoreCase(sortDir)
                ? cb.asc(root.get(sortBy))
                : cb.desc(root.get(sortBy));
        dataQuery.orderBy(order);

        // Sayfalama
        int page = req.getPage() != null ? req.getPage() : 0;
        int size = req.getSize() != null ? Math.min(req.getSize(), 200) : 50;

        TypedQuery<Task> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        List<TaskResponse> content = typedQuery.getResultList()
                .stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", size > 0 ? (int) Math.ceil((double) total / size) : 1);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    // ─── Predicate üreteci ────────────────────────────────────────────────────

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<Task> root,
            UUID teamId,
            List<TaskFilterCriteria> filters
    ) {
        List<Predicate> preds = new ArrayList<>();

        // Takım filtresi her zaman zorunlu
        preds.add(cb.equal(root.get("team").get("id"), teamId));

        if (filters == null || filters.isEmpty()) return preds;

        for (TaskFilterCriteria f : filters) {
            if (f.getField() == null || f.getOperator() == null) continue;

            try {
                Predicate p = buildSinglePredicate(cb, root, f);
                if (p != null) preds.add(p);
            } catch (Exception ignored) {
                // Geçersiz alan/operator'ü sessizce atla
            }
        }
        return preds;
    }

    private Predicate buildSinglePredicate(CriteriaBuilder cb, Root<Task> root, TaskFilterCriteria f) {
        String field = f.getField();
        String op = f.getOperator();
        List<String> vals = f.getValues();
        String firstVal = (vals != null && !vals.isEmpty()) ? vals.get(0) : null;

        // labels ve watchers @ElementCollection — substring veya join gerektirir,
        // basitlik adına LIKE kullanılır (small datasets için yeterli)
        if ("labels".equals(field)) {
            if ("contains".equals(op) && firstVal != null) {
                Join<Task, String> join = root.join("labels", JoinType.LEFT);
                return cb.equal(join, firstVal);
            }
            return null;
        }

        // Sprint ID özel durumu
        if ("sprintId".equals(field)) {
            if ("eq".equals(op) && firstVal != null) {
                return cb.equal(root.get("sprint").get("id"), UUID.fromString(firstVal));
            }
            if ("is_null".equals(op)) {
                return cb.isNull(root.get("sprint"));
            }
            if ("is_not_null".equals(op)) {
                return cb.isNotNull(root.get("sprint"));
            }
            return null;
        }

        // Tarih alanları
        if ("dueDate".equals(field) || "startDate".equals(field)) {
            Expression<LocalDate> expr = root.get(field);
            if ("gt".equals(op) && firstVal != null) return cb.greaterThan(expr, LocalDate.parse(firstVal));
            if ("lt".equals(op) && firstVal != null) return cb.lessThan(expr, LocalDate.parse(firstVal));
            if ("eq".equals(op) && firstVal != null) return cb.equal(expr, LocalDate.parse(firstVal));
            if ("is_null".equals(op)) return cb.isNull(expr);
            if ("is_not_null".equals(op)) return cb.isNotNull(expr);
            return null;
        }

        // String alanlar: status, priority, issueType, assignee, reporter, environment, resolution
        Expression<String> strExpr = root.get(field);
        return switch (op) {
            case "eq"         -> firstVal != null ? cb.equal(strExpr, firstVal) : null;
            case "neq"        -> firstVal != null ? cb.notEqual(strExpr, firstVal) : null;
            case "contains"   -> firstVal != null ? cb.like(cb.lower(strExpr), "%" + firstVal.toLowerCase() + "%") : null;
            case "in"         -> vals != null && !vals.isEmpty() ? strExpr.in(vals) : null;
            case "is_null"    -> cb.isNull(strExpr);
            case "is_not_null"-> cb.isNotNull(strExpr);
            default           -> null;
        };
    }
}


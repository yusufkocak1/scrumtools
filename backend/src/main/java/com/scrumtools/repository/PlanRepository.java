package com.scrumtools.repository;

import com.scrumtools.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Plan> findFirstByIsDefaultTrue();

    List<Plan> findByActiveTrueAndIsPublicTrueOrderBySortOrderAsc();

    List<Plan> findAllByOrderBySortOrderAsc();
}

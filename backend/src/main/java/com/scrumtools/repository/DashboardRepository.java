package com.scrumtools.repository;

import com.scrumtools.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    /**
     * Kullanıcının bir takımda görebildiği panolar: kendininkiler + takıma açılanlar.
     * Sıra sekme sırasıdır; eşitlikte oluşturulma zamanı belirler, böylece liste
     * her yüklemede aynı gelir.
     */
    @Query("SELECT d FROM Dashboard d WHERE d.team.id = :teamId " +
           "AND (d.owner.id = :userId " +
           "     OR d.visibility = com.scrumtools.entity.enums.DashboardVisibility.TEAM) " +
           "ORDER BY d.position ASC, d.createdAt ASC")
    List<Dashboard> findVisibleForUser(@Param("teamId") UUID teamId, @Param("userId") UUID userId);

    long countByTeamIdAndOwnerId(UUID teamId, UUID ownerId);

    boolean existsByTeamIdAndOwnerIdAndNameIgnoreCase(UUID teamId, UUID ownerId, String name);
}

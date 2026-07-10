package com.scrumtools.repository;

import com.scrumtools.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.email = :email")
    List<Team> findByMemberEmail(@Param("email") String email);

    Optional<Team> findByTeamCode(String teamCode);

    boolean existsByTeamCode(String teamCode);

    List<Team> findByOrganizationId(UUID organizationId);

    List<Team> findByOrganizationIsNull();

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE t.organization.id = :orgId AND LOWER(t.teamName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Team> searchByOrganizationIdAndName(@Param("orgId") UUID orgId, @Param("query") String query);

    List<Team> findByOrganizationIdAndTeamNameContainingIgnoreCaseOrderByTeamName(UUID organizationId, String teamName);
}


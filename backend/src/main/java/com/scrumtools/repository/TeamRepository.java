package com.scrumtools.repository;

import com.scrumtools.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.email = :email")
    List<Team> findByMemberEmail(@Param("email") String email);

    /** Task customId sayacını güncellerken race condition'a karşı kilitli okuma. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Team t WHERE t.id = :id")
    Optional<Team> findByIdForUpdate(@Param("id") UUID id);

    Optional<Team> findByTeamCode(String teamCode);

    boolean existsByTeamCode(String teamCode);

    List<Team> findByOrganizationId(UUID organizationId);

    /** Birincil projesi verilen proje olan takımlar. */
    List<Team> findByProjectId(UUID projectId);

    /**
     * Projede çalışan tüm takımlar — birincil bağ (teams.project_id) ya da çoklu bağ
     * (team_projects) üzerinden. Bir takım birden fazla projede çalışabildiği için
     * proje kapsamlı aramalarda (ör. commit → task eşleştirme) bu sorgu kullanılmalı.
     */
    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN t.projects p " +
           "WHERE t.project.id = :projectId OR p.id = :projectId")
    List<Team> findAllWorkingOnProject(@Param("projectId") UUID projectId);

    List<Team> findByOrganizationIsNull();

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE t.organization.id = :orgId AND LOWER(t.teamName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Team> searchByOrganizationIdAndName(@Param("orgId") UUID orgId, @Param("query") String query);

    List<Team> findByOrganizationIdAndTeamNameContainingIgnoreCaseOrderByTeamName(UUID organizationId, String teamName);
}


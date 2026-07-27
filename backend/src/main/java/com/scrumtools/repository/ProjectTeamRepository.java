package com.scrumtools.repository;

import com.scrumtools.entity.ProjectTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, UUID> {

    List<ProjectTeam> findByProjectId(UUID projectId);

    /** Takımın bağlı olduğu projeler — takım üyeliği değiştiğinde senkronlanacak hedefler. */
    List<ProjectTeam> findByTeamId(UUID teamId);

    Optional<ProjectTeam> findByProjectIdAndTeamId(UUID projectId, UUID teamId);

    boolean existsByProjectIdAndTeamId(UUID projectId, UUID teamId);

    void deleteByProjectIdAndTeamId(UUID projectId, UUID teamId);

    /**
     * Kullanıcıyı bu projeye taşıyan, verilen takım dışındaki takımlar.
     * Tam senkronda "başka bir bağlı takımda hâlâ üye mi?" sorusunu yanıtlar —
     * üyeyi projeden çıkarmadan önce bakılır.
     */
    @Query("""
            SELECT pt FROM ProjectTeam pt
            WHERE pt.project.id = :projectId
              AND pt.team.id <> :excludedTeamId
              AND EXISTS (SELECT 1 FROM TeamMember tm
                          WHERE tm.team.id = pt.team.id AND LOWER(tm.email) = LOWER(:email))
            """)
    List<ProjectTeam> findOtherLinksCoveringMember(@Param("projectId") UUID projectId,
                                                   @Param("excludedTeamId") UUID excludedTeamId,
                                                   @Param("email") String email);
}

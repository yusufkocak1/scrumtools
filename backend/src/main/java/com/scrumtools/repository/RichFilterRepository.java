package com.scrumtools.repository;

import com.scrumtools.entity.RichFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RichFilterRepository extends JpaRepository<RichFilter, UUID> {

    /** Kullanıcının bir takımda görebildiği zengin filtreler: kendininkiler + paylaşılanlar. */
    @Query("SELECT r FROM RichFilter r WHERE r.team.id = :teamId " +
           "AND (r.owner.email = :email OR r.visibility <> com.scrumtools.entity.enums.FilterVisibility.PRIVATE) " +
           "ORDER BY r.name")
    List<RichFilter> findVisibleForUser(@Param("teamId") UUID teamId, @Param("email") String email);

    /** Projeye açılmış zengin filtreler — PROJECT görünürlüğü takım sınırını aşar. */
    @Query("SELECT r FROM RichFilter r WHERE r.project.id = :projectId " +
           "AND r.visibility = com.scrumtools.entity.enums.FilterVisibility.PROJECT " +
           "AND r.team.id <> :excludeTeamId ORDER BY r.name")
    List<RichFilter> findProjectSharedOutsideTeam(@Param("projectId") UUID projectId,
                                                  @Param("excludeTeamId") UUID excludeTeamId);

    /**
     * STQL'deki {@code smart["ad"]} yazımının çözüm kaynağı — ad takım içinde benzersizdir.
     * Öğeler ayrıca çekilir; buradaki JOIN FETCH tek sorguda getirir.
     */
    @Query("SELECT DISTINCT r FROM RichFilter r LEFT JOIN FETCH r.elements " +
           "WHERE r.team.id = :teamId AND LOWER(r.name) = LOWER(:name)")
    Optional<RichFilter> findByTeamAndNameWithElements(@Param("teamId") UUID teamId,
                                                       @Param("name") String name);

    @Query("SELECT DISTINCT r FROM RichFilter r LEFT JOIN FETCH r.elements WHERE r.id = :id")
    Optional<RichFilter> findByIdWithElements(@Param("id") UUID id);

    boolean existsByTeamIdAndNameIgnoreCase(UUID teamId, String name);

    long countByTeamId(UUID teamId);
}

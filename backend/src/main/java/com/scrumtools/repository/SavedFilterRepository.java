package com.scrumtools.repository;

import com.scrumtools.entity.SavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SavedFilterRepository extends JpaRepository<SavedFilter, UUID> {

    /**
     * Kullanıcının bir takımda görebildiği filtreler: kendi kayıtları
     * ve takım/proje geneline açılmış olanlar.
     */
    @Query("SELECT f FROM SavedFilter f WHERE f.team.id = :teamId " +
           "AND (f.owner.email = :email OR f.visibility <> com.scrumtools.entity.enums.FilterVisibility.PRIVATE) " +
           "ORDER BY f.name")
    List<SavedFilter> findVisibleForUser(@Param("teamId") UUID teamId, @Param("email") String email);

    /**
     * Projeye açılmış filtreler — kullanıcının o projede çalışan başka bir
     * takımdan da erişebilmesi için (PROJECT görünürlüğü takım sınırını aşar).
     */
    @Query("SELECT f FROM SavedFilter f WHERE f.project.id = :projectId " +
           "AND f.visibility = com.scrumtools.entity.enums.FilterVisibility.PROJECT " +
           "AND f.team.id <> :excludeTeamId ORDER BY f.name")
    List<SavedFilter> findProjectSharedOutsideTeam(@Param("projectId") UUID projectId,
                                                   @Param("excludeTeamId") UUID excludeTeamId);

    boolean existsByTeamIdAndNameIgnoreCaseAndOwnerId(UUID teamId, String name, UUID ownerId);

    long countByOwnerId(UUID ownerId);
}

package com.scrumtools.repository;

import com.scrumtools.entity.TaskHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {
    List<TaskHistory> findByTaskIdOrderByChangedAtDesc(UUID taskId);

    /**
     * Bir takımdaki alan değişiklikleri, yeniden eskiye.
     *
     * Zaman serisi backfill'i geçmişi bugünden geriye doğru kurgular; değişiklikleri
     * ters uygulamak için tam bu sıra gerekir (bkz. RICH_FILTER_PLAN.md — K13).
     * Yalnız ihtiyaç duyulan alanlar çekilir: bir görevin başlık değişikliği,
     * durum serisini ilgilendirmez.
     *
     * @return satır başına [taskId, field, oldValue, changedAt]
     */
    @Query("SELECT h.task.id, h.field, h.oldValue, h.changedAt FROM TaskHistory h " +
           "WHERE h.task.team.id = :teamId AND h.field IN :fields AND h.changedAt > :from " +
           "ORDER BY h.changedAt DESC")
    List<Object[]> findTeamChanges(@Param("teamId") UUID teamId,
                                   @Param("fields") Collection<String> fields,
                                   @Param("from") LocalDateTime from,
                                   Pageable pageable);

    /** Proje kapsamlı zengin filtreler için aynı sorgunun daraltılmış hâli. */
    @Query("SELECT h.task.id, h.field, h.oldValue, h.changedAt FROM TaskHistory h " +
           "WHERE h.task.team.id = :teamId AND h.task.project.id = :projectId " +
           "AND h.field IN :fields AND h.changedAt > :from " +
           "ORDER BY h.changedAt DESC")
    List<Object[]> findProjectChanges(@Param("teamId") UUID teamId,
                                      @Param("projectId") UUID projectId,
                                      @Param("fields") Collection<String> fields,
                                      @Param("from") LocalDateTime from,
                                      Pageable pageable);
}


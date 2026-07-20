package com.scrumtools.repository;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByTeamIdAndStatusNot(UUID teamId, String status);

    List<Task> findByTeamId(UUID teamId);

    // ─── Proje bazlı listeleme (aktif proje context'i) ────────────────────────

    List<Task> findByTeamIdAndProjectId(UUID teamId, UUID projectId);

    List<Task> findByTeamIdAndProjectIdAndStatusNot(UUID teamId, UUID projectId, String status);

    long countByTeamIdAndProjectId(UUID teamId, UUID projectId);

    Optional<Task> findByTeamIdAndCustomId(UUID teamId, String customId);

    List<Task> findByTeamIdAndSprintId(UUID teamId, UUID sprintId);

    /**
     * Kullanıcının üye olduğu takımlardaki task'ı customId ile bulur (cross-team arama).
     */
    @Query("SELECT t FROM Task t WHERE t.customId = :customId AND t.team.id IN :teamIds")
    Optional<Task> findByCustomIdInTeams(String customId, List<UUID> teamIds);

    /** Takım içi typeahead arama — customId veya başlıkta geçen ifade (Cancelled hariç). */
    @Query("SELECT t FROM Task t WHERE t.team.id = :teamId AND t.status <> 'Cancelled' " +
           "AND (LOWER(t.customId) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY t.customId")
    List<Task> searchByCustomIdOrTitle(@Param("teamId") UUID teamId, @Param("q") String q, Pageable pageable);

    /** Takımdaki tüm customId'ler — kalıcı sayaç başlatılırken max sonek hesaplamak için. */
    @Query("SELECT t.customId FROM Task t WHERE t.team.id = :teamId")
    List<String> findCustomIdsByTeamId(@Param("teamId") UUID teamId);

    /** Projedeki tüm customId'ler — proje sayacı başlatılırken max sonek hesaplamak için. */
    @Query("SELECT t.customId FROM Task t WHERE t.project.id = :projectId")
    List<String> findCustomIdsByProjectId(@Param("projectId") UUID projectId);

    /** Takım projeye bağlanınca projesiz görevleri projeye ata (customId'ler korunur). */
    @Modifying
    @Query("UPDATE Task t SET t.project = :project WHERE t.team.id = :teamId AND t.project IS NULL")
    int assignProjectToTeamTasks(@Param("teamId") UUID teamId, @Param("project") Project project);

    // ─── Release sorguları ────────────────────────────────────────────────────

    List<Task> findByReleaseId(UUID releaseId);

    long countByReleaseId(UUID releaseId);

    long countByReleaseIdAndStatus(UUID releaseId, String status);

    // ─── Rapor sorguları ──────────────────────────────────────────────────────

    /** Status → count */
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.team.id = :teamId GROUP BY t.status")
    List<Object[]> countByStatus(@Param("teamId") UUID teamId);

    /** Priority → count */
    @Query("SELECT t.priority, COUNT(t) FROM Task t WHERE t.team.id = :teamId GROUP BY t.priority")
    List<Object[]> countByPriority(@Param("teamId") UUID teamId);

    /** IssueType → count */
    @Query("SELECT t.issueType, COUNT(t) FROM Task t WHERE t.team.id = :teamId GROUP BY t.issueType")
    List<Object[]> countByIssueType(@Param("teamId") UUID teamId);

    /** Vadesi geçmiş (dueDate < today, status NOT in Done/Cancelled) */
    @Query("SELECT t FROM Task t WHERE t.team.id = :teamId AND t.dueDate < :today " +
           "AND t.status NOT IN ('Done', 'Cancelled')")
    List<Task> findOverdue(@Param("teamId") UUID teamId, @Param("today") LocalDate today);

    /** Sprint'teki tasklar: sadece id, status, storyPoints (burndown için) */
    @Query("SELECT t FROM Task t WHERE t.sprint.id = :sprintId")
    List<Task> findBySprintId(@Param("sprintId") UUID sprintId);

    /** Assignee → open task count + story points */
    @Query("SELECT t.assignee, CAST(COUNT(t) AS int), CAST(COALESCE(SUM(t.storyPoints),0) AS int) " +
           "FROM Task t WHERE t.team.id = :teamId AND t.status NOT IN ('Done','Cancelled') " +
           "AND t.assignee IS NOT NULL GROUP BY t.assignee")
    List<Object[]> workloadByAssignee(@Param("teamId") UUID teamId);

    /** Created vs Resolved — son N gün */
    @Query("SELECT CAST(t.createdAt AS LocalDate), COUNT(t) FROM Task t " +
           "WHERE t.team.id = :teamId AND t.createdAt >= :since GROUP BY CAST(t.createdAt AS LocalDate) ORDER BY 1")
    List<Object[]> countCreatedPerDay(@Param("teamId") UUID teamId, @Param("since") LocalDateTime since);

    @Query("SELECT CAST(t.resolvedAt AS LocalDate), COUNT(t) FROM Task t " +
           "WHERE t.team.id = :teamId AND t.resolvedAt >= :since AND t.resolvedAt IS NOT NULL " +
           "GROUP BY CAST(t.resolvedAt AS LocalDate) ORDER BY 1")
    List<Object[]> countResolvedPerDay(@Param("teamId") UUID teamId, @Param("since") LocalDateTime since);
}

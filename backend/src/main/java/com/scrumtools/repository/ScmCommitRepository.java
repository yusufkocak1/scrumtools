package com.scrumtools.repository;

import com.scrumtools.entity.ScmCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ScmCommitRepository extends JpaRepository<ScmCommit, UUID> {

    boolean existsByRepositoryIdAndSha(UUID repositoryId, String sha);

    List<ScmCommit> findByRepositoryId(UUID repositoryId);

    /** Task'a bağlı commit'ler — DevPanel için yeniden eskiye sıralı. */
    @Query("SELECT c FROM ScmCommit c JOIN c.tasks t WHERE t.id = :taskId ORDER BY c.authoredAt DESC")
    List<ScmCommit> findByTaskId(@Param("taskId") UUID taskId);
}

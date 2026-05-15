package com.scrumtools.repository;

import com.scrumtools.entity.TaskLink;
import com.scrumtools.entity.enums.LinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskLinkRepository extends JpaRepository<TaskLink, UUID> {
    @Query("SELECT l FROM TaskLink l WHERE l.sourceTask.id = :taskId OR l.targetTask.id = :taskId")
    List<TaskLink> findAllByTaskId(@Param("taskId") UUID taskId);

    boolean existsBySourceTaskIdAndTargetTaskIdAndLinkType(UUID sourceId, UUID targetId, LinkType linkType);
}


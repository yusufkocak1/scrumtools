package com.scrumtools.repository;

import com.scrumtools.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByProjectId(UUID projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    Optional<ProjectMember> findByProjectIdAndUserEmail(UUID projectId, String email);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserEmail(UUID projectId, String email);

    @Query("""
            SELECT pm FROM ProjectMember pm
            WHERE pm.project.id = :projectId
              AND (LOWER(pm.user.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(pm.user.email) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY pm.user.name
            """)
    List<ProjectMember> searchByProjectIdAndUserNameOrEmail(@Param("projectId") UUID projectId, @Param("query") String query);
}


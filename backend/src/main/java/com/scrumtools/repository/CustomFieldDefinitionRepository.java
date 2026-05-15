package com.scrumtools.repository;

import com.scrumtools.entity.CustomFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {
    List<CustomFieldDefinition> findByTeamIdOrderByPositionAsc(UUID teamId);
    List<CustomFieldDefinition> findByProjectIdOrderByPositionAsc(UUID projectId);
}


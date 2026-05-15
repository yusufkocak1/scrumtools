package com.scrumtools.repository;

import com.scrumtools.entity.DocSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocSpaceRepository extends JpaRepository<DocSpace, UUID> {

    List<DocSpace> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}


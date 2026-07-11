package com.scrumtools.repository;

import com.scrumtools.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    List<Release> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}

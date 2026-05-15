package com.scrumtools.repository;

import com.scrumtools.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByTeamId(UUID teamId);
    List<Sprint> findByTeamIdAndStatus(UUID teamId, String status);
}


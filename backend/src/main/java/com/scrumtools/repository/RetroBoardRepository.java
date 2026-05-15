package com.scrumtools.repository;

import com.scrumtools.entity.RetroBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetroBoardRepository extends JpaRepository<RetroBoard, UUID> {
    List<RetroBoard> findByTeamIdOrderByCreatedAtAsc(UUID teamId);
}


package com.scrumtools.repository;

import com.scrumtools.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByTeamId(UUID teamId);

    Optional<Board> findByTeamIdAndIsDefaultTrue(UUID teamId);
}


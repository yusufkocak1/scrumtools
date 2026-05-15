package com.scrumtools.repository;

import com.scrumtools.entity.RetroItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetroItemRepository extends JpaRepository<RetroItem, UUID> {
    List<RetroItem> findByRetroBoardIdAndColumnName(UUID boardId, String columnName);
    List<RetroItem> findByRetroBoardId(UUID boardId);
}


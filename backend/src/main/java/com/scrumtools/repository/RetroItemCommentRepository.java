package com.scrumtools.repository;

import com.scrumtools.entity.RetroItemComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RetroItemCommentRepository extends JpaRepository<RetroItemComment, UUID> {
}


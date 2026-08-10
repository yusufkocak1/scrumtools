package com.scrumtools.repository;

import com.scrumtools.entity.QuizImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizImageRepository extends JpaRepository<QuizImage, UUID> {
}

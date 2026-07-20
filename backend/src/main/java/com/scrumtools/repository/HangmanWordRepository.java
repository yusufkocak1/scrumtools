package com.scrumtools.repository;

import com.scrumtools.entity.HangmanWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HangmanWordRepository extends JpaRepository<HangmanWord, UUID> {
    List<HangmanWord> findByTeamIdAndLanguageOrderByCreatedAtDesc(UUID teamId, String language);

    boolean existsByTeamIdAndLanguageAndWordIgnoreCase(UUID teamId, String language, String word);
}

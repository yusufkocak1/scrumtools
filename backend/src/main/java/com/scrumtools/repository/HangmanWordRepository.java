package com.scrumtools.repository;

import com.scrumtools.entity.HangmanWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HangmanWordRepository extends JpaRepository<HangmanWord, UUID> {
    List<HangmanWord> findByLanguageOrderByCreatedAtDesc(String language);

    boolean existsByLanguageAndWordIgnoreCase(String language, String word);
}

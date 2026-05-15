package com.scrumtools.repository;

import com.scrumtools.entity.QuizAnswer;
import com.scrumtools.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    @Modifying
    @Query("DELETE FROM QuizAnswer a WHERE a.question IN :questions")
    void deleteByQuestionIn(Collection<QuizQuestion> questions);

    List<QuizAnswer> findBySessionIdAndQuestionId(UUID sessionId, UUID questionId);

    Optional<QuizAnswer> findBySessionIdAndQuestionIdAndUserEmail(UUID sessionId, UUID questionId, String userEmail);

    boolean existsBySessionIdAndQuestionIdAndUserEmail(UUID sessionId, UUID questionId, String userEmail);

    List<QuizAnswer> findBySessionIdAndUserEmailOrderByQuestion_QuestionOrderAsc(UUID sessionId, String userEmail);

    List<QuizAnswer> findBySessionId(UUID sessionId);
}


package com.example.course_server.repository;

import com.example.course_server.model.QuizSubmission;
import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Integer> {
    boolean existsByUserIdAndQuizId(Integer userId, Integer quizId);
    @EntityGraph(attributePaths = {"quiz", "answers", "answers.question", "answers.selectedOption"})
    Optional<QuizSubmission> findTopByUserIdOrderBySubmittedAtDesc(Integer userId);

    List<QuizSubmission> findByUserIdOrderBySubmittedAtDesc(Integer userId);
}

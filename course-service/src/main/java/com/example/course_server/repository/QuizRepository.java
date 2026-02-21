package com.example.course_server.repository;

import com.example.course_server.model.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId")
    Optional<Quiz> findBasicQuizByCourseId(@Param("courseId") Integer courseId);

    @EntityGraph(attributePaths = {"questions"})
    @Query("SELECT q FROM Quiz q WHERE q.id = :quizId")
    Optional<Quiz> findQuizWithQuestions(@Param("quizId") Integer quizId);

    Optional<Quiz> findByCourseId(Integer courseId);
}

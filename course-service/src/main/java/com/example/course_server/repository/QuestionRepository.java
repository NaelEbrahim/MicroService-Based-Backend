package com.example.course_server.repository;

import com.example.course_server.model.Option;
import com.example.course_server.model.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @EntityGraph(attributePaths = {"options"})
    List<Question> findByQuizId(Integer quizId);
}

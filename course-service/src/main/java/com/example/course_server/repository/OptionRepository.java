package com.example.course_server.repository;

import com.example.course_server.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Integer> {

    // In OptionRepository
    List<Option> findByQuestionIdIn(List<Integer> questionIds);
}

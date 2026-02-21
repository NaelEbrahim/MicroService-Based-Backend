package com.example.course_server.repository;

import com.example.course_server.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findByTrainerId(Integer trainerId);
}

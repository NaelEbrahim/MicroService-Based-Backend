package com.example.Enrollement_service.repository;

import com.example.Enrollement_service.model.Enrollment;
import com.example.Enrollement_service.model.PayedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    boolean existsByUserIdAndCourseId(Integer userId, Integer courseId);

    Optional<Enrollment> findByUserIdAndCourseId(Integer userId, Integer courseId);

    List<Enrollment> findAllByUserId(Integer userId);

    List<Enrollment> findAllByCourseId(Integer courseId);

     boolean existsByUserIdAndCourseIdAndPayedStatus(Integer userId, Integer courseId, PayedStatus payedStatus);

}
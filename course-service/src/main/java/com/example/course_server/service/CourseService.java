package com.example.course_server.service;

import com.example.course_server.client.UserClient;
import com.example.course_server.dto.*;
import com.example.course_server.model.Course;
import com.example.course_server.model.CourseStatus;
import com.example.course_server.repository.CourseRepository;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final JwtService jwtService;
    private final UserClient userClient;


    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<?> createCourse(CourseDTO dto) {
        try {
            Integer trainerId = getCurrentUserId();
            UserResponse trainer = fetchVerifiedUser(trainerId);
            if (trainer == null) {
                return buildServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(trainer.getId() == 0)) {
                // Means user was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + trainerId);
            }

            Course course = Course.builder()
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .trainerId(trainerId)
                    .status(CourseStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            courseRepository.save(course);

            return buildSuccessResponse("Course Request Submitted Successfully");

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", e.getMessage());
        }
    }

    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<?> updateCourse(Integer id, CourseDTO dto) {
        try {
            Integer trainerId = getCurrentUserId();
            UserResponse trainer = fetchVerifiedUser(trainerId);
            if (trainer == null) {
                return buildServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(trainer.getId() == 0)) {
                // Means user was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + trainerId);
            }

            Optional<Course> course = courseRepository.findById(id);
            if (course.isEmpty()) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found");
            }

            if (!course.get().getTrainerId().equals(trainerId)) {
                return buildErrorResponse(HttpStatus.FORBIDDEN, "You can only update your own courses");
            }


            if (dto.getTitle() != null && !dto.getTitle().equals(course.get().getTitle())) {
                course.get().setTitle(dto.getTitle());
            }
            if (dto.getDescription() != null && !dto.getDescription().equals(course.get().getDescription())) {
                course.get().setDescription(dto.getDescription());
            }
            courseRepository.save(course.get());

            return buildSuccessResponse("Course updated Successfully");

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(Integer id) {
        try {

            Optional<Course> course = courseRepository.findById(id);
            if (course.isEmpty()) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found");
            }

            courseRepository.delete(course.get());
            return buildSuccessResponse("Course Deleted Successfully");

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", e.getMessage());
        }
    }

    public ResponseEntity<?> getCourseById(Integer id) {
        Optional<Course> course = courseRepository.findById(id);
        if (course.isEmpty()) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found");
        }
        UserResponse trainer = fetchVerifiedUser(course.get().getTrainerId());
        if (trainer == null) {
            return buildServiceUnavailableResponse();
        }

        if (Boolean.TRUE.equals(trainer.getId() == 0)) {
            // Means user was not found (graceful 404)
            return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + course.get().getTrainerId());
        }
        return ResponseEntity.ok(buildCourseResponse(course.get()));
    }

    public ResponseEntity<?> getAllCourses() {
        try {

            List<CourseResponseDTO> courses = courseRepository.findAll().stream()
                    .map(this::buildCourseResponse)
                    .toList();

            return buildSuccessResponse("Courses retrieved", Map.of(
                    "count", courses.size(),
                    "courses", courses
            ));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveCourse(Integer id) {
        Optional<Course> course = courseRepository.findById(id);
        if (course.isEmpty()) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found");
        }
        course.get().setStatus(CourseStatus.APPROVED);
        courseRepository.save(course.get());

        return buildSuccessResponse("Course Approved Successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> rejectCourse(Integer id) {
        Optional<Course> course = courseRepository.findById(id);
        if (course.isEmpty()) {
             return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found");
        }
        course.get().setStatus(CourseStatus.REJECTED);
        courseRepository.save(course.get());

        return buildSuccessResponse("Course Rejected Successfully");

    }

    // ========== Helper Methods ========== //

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        return Math.toIntExact(Long.parseLong((jwtService.extractUserId(token))));
    }

    private UserResponse fetchVerifiedUser(Integer userId) {
        try {
            ResponseEntity<UserResponse> response = userClient.getUserById(userId);
            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                return response.getBody();
            }

            if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                log.warn("User not found for ID: {}", userId);
                return new UserResponse(0, "UNKNOWN","N/A", "N/A");  // Return a dummy course
            }
            log.warn("Failed to fetch user - UserID: {}, Status: {}", userId, response.getStatusCode());
            return null;
        } catch (FeignException.NotFound notFound) {
            log.warn("User not found (404) for userId {}: {}", userId, notFound.contentUTF8());
            return new UserResponse(0, "UNKNOWN","N/A", "N/A"); // Handle Feign 404
        }
        catch (Exception e) {
            log.error("Trainer verification failed", e);
            return null;
        }
    }



    // ========== Response Builders ========== //

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String message) {
        return buildSuccessResponse(message, null);
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String message, Map<String, Object> data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        if (data != null) response.putAll(data);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, message, null);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, String detail) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        if (detail != null) response.put("detail", detail);
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<Map<String, Object>> buildServiceUnavailableResponse() {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "User service unavailable",
                "All operations suspended until user service is restored");
    }


    private CourseResponseDTO buildCourseResponse(Course course) {
        UserResponse trainer = fetchVerifiedUser(course.getTrainerId());
        return CourseResponseDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .status(course.getStatus().name())
                .createdAt(course.getCreatedAt().toString())
                .trainer(trainer)
                .build();
    }


}
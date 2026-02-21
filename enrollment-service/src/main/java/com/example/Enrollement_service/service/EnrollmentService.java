package com.example.Enrollement_service.service;

import com.example.Enrollement_service.clients.CourseClient;
import com.example.Enrollement_service.clients.UserClient;
import com.example.Enrollement_service.dto.CourseResponse;
import com.example.Enrollement_service.dto.UserResponse;
import com.example.Enrollement_service.model.Enrollment;
import com.example.Enrollement_service.dto.EnrollmentRequest;
import com.example.Enrollement_service.dto.EnrollmentStatusUpdateDTO;
import com.example.Enrollement_service.model.EnrollmentStatus;
import com.example.Enrollement_service.model.PayedStatus;
import com.example.Enrollement_service.repository.EnrollmentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final JwtService jwtService;
    private final UserClient userClient;
    private final CourseClient courseClient;


    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<?> enrollStudent(EnrollmentRequest request) {
        try {

            Integer studentId = getCurrentUserId();
            UserResponse user = fetchVerifiedUser(studentId);
            if (user == null) {
                return buildServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(user.getId() == 0)) {
                // Means user was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + studentId);
            }


            CourseResponse course = fetchCourse(request.getCourseId());

            if (course == null) {
                // Means service is down or returned an unexpected error
                return buildCourseServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(course.getId() == 0)) {
                // Means course was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found", "Course ID: " + request.getCourseId());
            }


            if (enrollmentRepository.existsByUserIdAndCourseId(getCurrentUserId(), request.getCourseId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "User already enrolled in this course."));
            }

            Enrollment enrollment = Enrollment.builder()
                    .userId(getCurrentUserId())
                    .courseId(request.getCourseId())
                    .status(EnrollmentStatus.ENROLLED)
                    .payedStatus(PayedStatus.UNPAYED)
                    .enrolledAt(LocalDateTime.now())
                    .build();

            enrollmentRepository.save(enrollment);
            return buildSuccessResponse("Enrolled Student Successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error during enrollment: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<?> pay(EnrollmentRequest request) {
        try {
            Integer studentId = getCurrentUserId();
            UserResponse user = fetchVerifiedUser(studentId);
            if (user == null) {
                return buildServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(user.getId() == 0)) {
                // Means user was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + studentId);
            }

            CourseResponse course = fetchCourse(request.getCourseId());

            if (course == null) {
                // Means service is down or returned an unexpected error
                return buildCourseServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(course.getId() == 0)) {
                // Means course was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found", "Course ID: " + request.getCourseId());
            }

            var enrollment = enrollmentRepository.findByUserIdAndCourseId(getCurrentUserId(), request.getCourseId());
            if (enrollment.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "You haven't enrolled in this course."));
            }

            if (enrollment.get().getPayedStatus() == PayedStatus.PAYED) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Payment already completed for this course."));
            }

            enrollment.get().setPayedStatus(PayedStatus.PAYED);
            enrollmentRepository.save(enrollment.get());

            return buildSuccessResponse("Payment Successful");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error during payment: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> updateStatus(EnrollmentStatusUpdateDTO dto) {
        try {
            Integer studentId = dto.getUserId();
            UserResponse user = fetchVerifiedUser(studentId);
            if (user == null) {
                return buildServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(user.getId() == 0)) {
                // Means user was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + dto.getUserId());
            }

            CourseResponse course = fetchCourse(dto.getCourseId());

            if (course == null) {
                // Means service is down or returned an unexpected error
                return buildCourseServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(course.getId() == 0)) {
                // Means course was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found", "Course ID: " + dto.getCourseId());
            }



            Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(dto.getUserId(), dto.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Enrollment not found"));

            EnrollmentStatus newStatus = EnrollmentStatus.valueOf(dto.getStatus());
            enrollment.setStatus(newStatus);

            enrollmentRepository.save(enrollment);

            return ResponseEntity.ok(Map.of(
                    "message", "Status updated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid status value: " + dto.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating status: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> getAllEnrollmentsByUser(Integer userId) {
        try {
            UserResponse user = fetchVerifiedUser(userId);
            if (user == null) {
                return buildServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(user.getId() == 0)) {
                // Means user was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + userId);
            }

            var enrollments = enrollmentRepository.findAllByUserId(userId);
            if (enrollments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No enrollments found for this user"));
            }

            // Enhance response with course details
            List<Map<String, Object>> enrichedEnrollments = enrollments.stream()
                    .map(enrollment -> {
                        ResponseEntity<CourseResponse> response = courseClient.getCourseById(enrollment.getCourseId());
                        CourseResponse course = response != null && response.getStatusCode().is2xxSuccessful()
                                ? response.getBody()
                                : null;

                        return Map.of(
                                "enrollmentId", enrollment.getId(),
                                "course", course != null ? course : "Course details not available",
                                "status", enrollment.getStatus(),
                                "paymentStatus", enrollment.getPayedStatus(),
                                "enrolledAt", enrollment.getEnrolledAt()
                        );
                    })
                    .collect(Collectors.toList());


            return ResponseEntity.ok(enrichedEnrollments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving enrollments: " + e.getMessage()));
        }
    }

    public ResponseEntity<Boolean> checkEnrollmentExists(Integer userId, Integer courseId) {

         return ResponseEntity.status(HttpStatus.OK)
                 .body(enrollmentRepository.
                         existsByUserIdAndCourseIdAndPayedStatus(userId,courseId,PayedStatus.PAYED));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCourseReport(Integer courseId) {
        try {
            CourseResponse course = fetchCourse(courseId);

            if (course == null) {
                // Means service is down or returned an unexpected error
                return buildCourseServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(course.getId() == 0)) {
                // Means course was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found", "Course ID: " + courseId);
            }
            List<Enrollment> enrollments = enrollmentRepository.findAllByCourseId(courseId);
            if (enrollments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No enrollments found for this course"));
            }

            long total = enrollments.size();
            long passed = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.PASSED).count();
            long failed = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.FAILED).count();
            long active = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED).count();
            long paid = enrollments.stream().filter(e -> e.getPayedStatus() == PayedStatus.PAYED).count();

            return ResponseEntity.ok(Map.of(
                    "courseId", courseId,
                    "totalEnrollments", total,
                    "passed", passed,
                    "failed", failed,
                    "active", active,
                    "paidEnrollments", paid,
                    "passRate", total > 0 ? ((double) passed / total) * 100 : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error generating course report: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCourseEnrollments(Integer courseId) {
        try {
            CourseResponse course = fetchCourse(courseId);

            if (course == null) {
                // Means service is down or returned an unexpected error
                return buildCourseServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(course.getId() == 0)) {
                // Means course was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Course not found", "Course ID: " + courseId);
            }

            List<Enrollment> enrollments = enrollmentRepository.findAllByCourseId(courseId);
            if (enrollments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No enrollments found for this course"));
            }

            // Enhance enrollments with user details
            List<Map<String, Object>> enrichedEnrollments = enrollments.stream()
                    .map(enrollment -> {
                        ResponseEntity<UserResponse> response = userClient.getUserById(enrollment.getUserId());
                        UserResponse user = response != null && response.getStatusCode().is2xxSuccessful()
                                ? response.getBody()
                                : null;

                        return Map.of(
                                "enrollmentId", enrollment.getId(),
                                "user", user != null ? user : "User details not available",
                                "status", enrollment.getStatus(),
                                "paymentStatus", enrollment.getPayedStatus(),
                                "enrolledAt", enrollment.getEnrolledAt()
                        );

                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(enrichedEnrollments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving course enrollments: " + e.getMessage()));
        }
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

    private CourseResponse fetchCourse(Integer courseId) {
        try {
            ResponseEntity<CourseResponse> response = courseClient.getCourseById(courseId);

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                return response.getBody();
            }

            if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                log.warn("Course not found for ID: {}", courseId);
                return new CourseResponse(0, "UNKNOWN", "N/A");  // Return a dummy course
            }

            log.warn("Unexpected response from Course service. Status: {}, CourseID: {}", response.getStatusCode(), courseId);
            return null;

        } catch (FeignException.NotFound notFound) {
            log.warn("Course not found (404) for courseId {}: {}", courseId, notFound.contentUTF8());
            return new CourseResponse(0, "UNKNOWN", "N/A"); // Handle Feign 404
        } catch (Exception e) {
            log.error("Exception while fetching course {}: {}", courseId, e.getMessage());
            return null;  // service down or internal error
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

    private ResponseEntity<Map<String, Object>> buildCourseServiceUnavailableResponse() {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Course service unavailable",
                "All operations suspended until course service is restored");
    }
}
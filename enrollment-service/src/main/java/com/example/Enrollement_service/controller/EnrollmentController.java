package com.example.Enrollement_service.controller;

import com.example.Enrollement_service.dto.EnrollmentRequest;
import com.example.Enrollement_service.dto.EnrollmentStatusUpdateDTO;
import com.example.Enrollement_service.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody EnrollmentRequest request) {
        return enrollmentService.enrollStudent(request);
    }
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody EnrollmentRequest request) {
        return enrollmentService.pay(request);
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestBody EnrollmentStatusUpdateDTO dto) {
        return enrollmentService.updateStatus(dto);
    }

    @GetMapping("/{userId}/status/{courseId}")
    public ResponseEntity<Boolean> getEnrollmentStatus(@PathVariable Integer userId ,@PathVariable Integer courseId) {
        return enrollmentService.checkEnrollmentExists(userId,courseId);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getEnrollments(@PathVariable Integer userId) {
        return enrollmentService.getAllEnrollmentsByUser(userId);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseEnrollments(@PathVariable Integer courseId) {
        return enrollmentService.getCourseEnrollments(courseId);
    }

    @GetMapping("/course/{courseId}/report")
    public ResponseEntity<?> getCourseReport(@PathVariable Integer courseId) {
        return enrollmentService.getCourseReport(courseId);
    }

}

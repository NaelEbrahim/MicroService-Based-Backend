// ✅ Updated fallback for EnrollmentClient
package com.example.course_server.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EnrollmentClientFallback implements EnrollmentClient {

    @Override
    public void updateEnrollmentStatus(EnrollmentStatusUpdateDTO dto) {
        log.warn("[Fallback] Failed to update enrollment status for userId={}, courseId={}, status={}",
                dto.getUserId(), dto.getCourseId(), dto.getStatus());
    }

    @Override
    public ResponseEntity<Boolean> getEnrollmentStatus(Integer userId, Integer courseId) {
        log.warn("[Fallback] Could not fetch enrollment status for userId={}, courseId={}", userId, courseId);
         return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(false); // return default: not enrolled
    }
}

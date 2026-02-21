// ✅ CourseClient fallback implementation
package com.example.Enrollement_service.clients;

import com.example.Enrollement_service.dto.CourseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CourseClientFallback implements CourseClient {

    @Override
    public ResponseEntity<CourseResponse> getCourseById(Integer id) {
        log.warn("Fallback triggered for getCourseById, returning dummy data");

        // Create a default response
        CourseResponse defaultCourse = new CourseResponse(
                id,
                "Unavailable",
                "N/A"
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(defaultCourse);
    }
}
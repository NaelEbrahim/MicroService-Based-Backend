
package com.example.course_server.client;

import com.example.course_server.config.FeignConfig;
import lombok.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "enrollment-service"
            , url = "${application.config.enrollment-service-url}"
            , fallback = EnrollmentClientFallback.class
            ,configuration = FeignConfig.class)
public interface EnrollmentClient {

    @PostMapping("/api/enrollments/status")
    void updateEnrollmentStatus(@RequestBody EnrollmentStatusUpdateDTO dto);

    @GetMapping("api/enrollments/{userId}/status/{courseId}")
    ResponseEntity<Boolean> getEnrollmentStatus(@PathVariable("userId") Integer userId, @PathVariable("courseId") Integer courseId);

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class EnrollmentStatusUpdateDTO {
        private Integer userId;
        private Integer courseId;
        private String status; // PASSED or FAILED
    }
}

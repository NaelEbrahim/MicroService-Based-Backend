package com.example.Enrollement_service.clients;

import com.example.Enrollement_service.config.FeignConfig;
import com.example.Enrollement_service.dto.CourseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "course-service"
            ,url = "${application.config.course-service-url}"
            , fallback = CourseClientFallback.class
            ,configuration = FeignConfig.class)
public interface CourseClient {

    @GetMapping("/api/courses/{id}")
    ResponseEntity<CourseResponse> getCourseById(@PathVariable("id") Integer id);


}

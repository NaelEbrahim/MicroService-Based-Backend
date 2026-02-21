package com.example.course_server.client;

import com.example.course_server.config.FeignConfig;
import com.example.course_server.dto.UserResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service"
        , url = "${application.config.user-service-url}"
        ,fallback = UserClientFallback.class
        ,configuration = FeignConfig.class)

public interface UserClient {

    @GetMapping("/api/users/{id}")
    ResponseEntity<UserResponse> getUserById(@PathVariable("id") Integer id);


}



package com.example.Enrollement_service.clients;

import com.example.Enrollement_service.config.FeignConfig;
import com.example.Enrollement_service.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "user-service"
            , url = "${application.config.user-service-url}"
            , fallback = UserClientFallback.class
            ,configuration = FeignConfig.class)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    ResponseEntity<UserResponse> getUserById(@PathVariable("id") Integer id);


}

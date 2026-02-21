// ✅ UserClient fallback implementation
package com.example.course_server.client;

import com.example.course_server.dto.UserResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
public class UserClientFallback implements UserClient {
    @Override
    public ResponseEntity<UserResponse> getUserById(Integer id) {
        log.warn("FALLBACK ACTIVATED: User service unavailable - returning default response for user {}", id);

        // Create a default response
        UserResponse defaultUser = new UserResponse(
                id,
                "Service Unavailable",
                "N/A",
                "UNKNOWN"
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(defaultUser);
    }
}

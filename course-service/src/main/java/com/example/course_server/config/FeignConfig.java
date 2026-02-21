package com.example.course_server.config;

import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.naming.ServiceUnavailableException;

@Configuration
public class FeignConfig {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000, 2000, 3); // Wait 1s, max 2s, 3 attempts
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            if (response.status() >= 500) {
                return new ServiceUnavailableException("User service unavailable");
            }
            return new ErrorDecoder.Default().decode(methodKey, response);
        };
    }

    @Bean
    public RequestInterceptor bearerTokenInterceptor() {
        return template -> {
            // Get current authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getCredentials() instanceof String) {
                String token = (String) authentication.getCredentials();
                template.header("Authorization", "Bearer " + token);
            }
        };
    }
}



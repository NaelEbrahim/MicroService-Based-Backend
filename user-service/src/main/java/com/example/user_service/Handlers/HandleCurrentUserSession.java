package com.example.user_service.Handlers;

import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.GeneralServices.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class HandleCurrentUserSession {

    private static UserRepository userRepository;

    private static JwtService jwtService;


    public HandleCurrentUserSession(UserRepository userRepository, JwtService jwtService) {
        HandleCurrentUserSession.jwtService = jwtService;
        HandleCurrentUserSession.userRepository = userRepository;
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
            throw new UsernameNotFoundException("Invalid authentication or token");
        }
        Integer userId = Integer.parseInt(jwtService.extractId(token));
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
    }

}

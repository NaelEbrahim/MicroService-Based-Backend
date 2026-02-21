package com.example.user_service.service;

import com.example.user_service.Config.SecurityConfig;
import com.example.user_service.Handlers.HandleCurrentUserSession;
import com.example.user_service.dto.*;
import com.example.user_service.model.AuthToken;
import com.example.user_service.model.Enum.Roles;
import com.example.user_service.model.Role;
import com.example.user_service.model.User;
import com.example.user_service.model.User_Role;
import com.example.user_service.repository.AuthTokenRepository;
import com.example.user_service.repository.RoleRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.repository.User_RoleRepository;
import com.example.user_service.service.GeneralServices.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private final AuthTokenRepository authTokenRepository;

    private final RoleRepository roleRepository;

    private final User_RoleRepository userRoleRepository;

    private final SecurityConfig securityConfig;

    private final JwtService jwtService;

    public ResponseEntity<?> register(RegisterDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message:", "email already exist"));
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(securityConfig.passwordEncoder().encode(request.getPassword()));
        userRepository.save(user);
        Optional<Role> role = Optional.of(roleRepository.findByRoleName(Roles.LEARNER).orElseGet(()
                -> roleRepository.save(Role.builder().roleName(Roles.LEARNER).build())));;
        userRoleRepository.save(User_Role.builder()
                .user(user)
                .role(role.get())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<?> createUser(UserDTO createRequest) throws Exception {
        return internalCreateUser(createRequest);
    }

    @Transactional
    public ResponseEntity<?> internalCreateUser(UserDTO createRequest) throws Exception {
        if (userRepository.findByEmail(createRequest.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message:", "email already exist"));
        }
        // Create User
        User newUser = new User();
        newUser.setName(createRequest.getName());
        newUser.setEmail(createRequest.getEmail());
        newUser.setPassword(securityConfig.passwordEncoder().encode(createRequest.getPassword()));
        userRepository.save(newUser);
        //Roles
        for (Roles roleName : createRequest.getRoles()) {
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).build()));
            userRoleRepository.save(User_Role.builder()
                    .user(newUser)
                    .role(role)
                    .build());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User Created Successfully"));
    }

    @Transactional
    public ResponseEntity<?> login(LoginRequest loginRequest, HttpServletResponse response) {
        var user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
        if (user != null && securityConfig.passwordEncoder().matches(loginRequest.getPassword(), user.getPassword())) {
            //Fetch User Roles
            var ur = userRoleRepository.findByUserId(user.getId());
            List<Roles> userRoles = new ArrayList<>();
            for (User_Role element : ur)
                userRoles.add(element.getRole().getRoleName());
            // Tokens
            invalidateUserToken(user.getId());
            String accessToken = jwtService.generateAccessToken(user, userRoles);
            authTokenRepository.save(AuthToken.builder().user(user).accessToken(accessToken).build());
            String refreshToken = jwtService.generateRefreshToken(user);
            var cookie = new Cookie("refreshToken", refreshToken);
            cookie.setSecure(true);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(259200);
            cookie.setPath("/auth/refresh");
            response.addCookie(cookie);
            UserResponse userResponse = UserResponse.mapToUserResponse(user, accessToken);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", userResponse));
        } else
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid email or password"));
    }

    @Transactional
    public ResponseEntity<?> logout() {
        var user = HandleCurrentUserSession.getCurrentUser();
        if (user != null) {
            invalidateUserToken(user.getId());
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "logout successfully"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "something went wrong"));
    }

    @Transactional
    public void invalidateUserToken(Integer userId) {
        authTokenRepository.deleteByUserId(userId);
        authTokenRepository.flush();
    }


    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<?> getUsersByRole(Roles roleName) {
        Optional<Role> roleOptional = roleRepository.findByRoleName(roleName);

        if (roleOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Role not found"));
        }

        Role role = roleOptional.get();

        // Fetch User_Role records and map to UserResponse
        List<UserResponse> userResponses = userRoleRepository.findByRoleId(role.getId())
                .stream()
                .map(ur -> UserResponse.mapToUserResponse(ur.getUser(),null))
                .toList();

        // Build a structured response object (optional wrapper)
        Map<String, Object> response = Map.of(
                "role", role.getRoleName(),
                "count", userResponses.size(),
                "users", userResponses
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<?> getAll() {
        List<UserResponse> userResponses = userRepository.findAll()
                .stream()
                .map(u -> UserResponse.mapToUserResponse(u,null))
                .toList();
        Map<String, Object> response = Map.of(
                "count", userResponses.size(),

                "users", userResponses
        );
        return ResponseEntity.ok(response);

    }

    private boolean userHasRole(User user, Roles roleName) {
        return userRoleRepository.findByUserId(user.getId()).stream()
                .anyMatch(ur -> roleName.name().equalsIgnoreCase(String.valueOf(ur.getRole().getRoleName())));
    }

    public ResponseEntity<?> getUserById(Integer id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
        UserResponse userResponse = UserResponse.mapToUserResponse(userOptional.get(),null);
        return ResponseEntity.ok(userResponse);
    }
}
package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterDto;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDto dto) {
        return userService.register(dto);
    }

    @PostMapping("/add-user")
    public ResponseEntity<?> addUser(@Valid @RequestBody UserDTO dto) throws Exception {
        return userService.createUser(dto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,  HttpServletResponse response) {
        return userService.login(request, response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Integer id){
        return userService.getUserById(id);
    };
    @GetMapping
    public ResponseEntity<?> getAll() {
        return userService.getAll();
    }
}

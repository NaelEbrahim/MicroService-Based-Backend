package com.example.user_service.Config;

import com.example.user_service.dto.UserDTO;
import com.example.user_service.model.Enum.Roles;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            UserDTO admin = new UserDTO();
            admin.setName("yasser_Marzouk");
            admin.setEmail("yasser@gmail.com");
            admin.setPassword("12345678");
            List<Roles> roles = new ArrayList<>();
            roles.add(Roles.ADMIN);
            admin.setRoles(roles);
            userService.internalCreateUser(admin);
        }
    }
}
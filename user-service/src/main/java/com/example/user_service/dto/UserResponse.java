package com.example.user_service.dto;
import com.example.user_service.model.User;
import com.example.user_service.model.User_Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer id;
    private String name;
    private String email;
    private String role;
    private String accessToken;

    public static UserResponse mapToUserResponse(User user, String accessToken) {
        String roleName = user.getUserRoleList().stream()
                .map(User_Role::getRole)
                .filter(Objects::nonNull)
                .map(role -> role.getRoleName().name())
                .findFirst()
                .orElse(null);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                roleName,
                accessToken
        );
    }


}

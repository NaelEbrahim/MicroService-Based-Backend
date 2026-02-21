package com.example.user_service.repository;

import com.example.user_service.model.Enum.Roles;
import com.example.user_service.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Integer> {

    Optional<Role> findByRoleName (Roles roleName);

}

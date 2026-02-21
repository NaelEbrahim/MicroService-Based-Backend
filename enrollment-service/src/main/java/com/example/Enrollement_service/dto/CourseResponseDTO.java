package com.example.Enrollement_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseResponseDTO {
    private Integer id;
    private String title;
    private String description;
    private String status;
    private String createdAt;

    private UserResponse trainer;
}

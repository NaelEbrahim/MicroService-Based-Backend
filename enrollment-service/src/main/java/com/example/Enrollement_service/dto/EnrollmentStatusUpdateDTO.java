package com.example.Enrollement_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatusUpdateDTO {
    private Integer userId;
    private Integer courseId;
    private String status;
}

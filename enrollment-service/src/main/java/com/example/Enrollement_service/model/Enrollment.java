package com.example.Enrollement_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer userId;
    private Integer courseId;
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;
    @Enumerated(EnumType.STRING)
    private PayedStatus payedStatus;
    private LocalDateTime enrolledAt;
}

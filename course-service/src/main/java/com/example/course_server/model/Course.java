package com.example.course_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    private String description;

    // In your Course entity
    @Column(nullable = false) // Add if missing
    private Integer trainerId;

    @Enumerated(EnumType.STRING)
    private CourseStatus status = CourseStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private Quiz quiz;

}

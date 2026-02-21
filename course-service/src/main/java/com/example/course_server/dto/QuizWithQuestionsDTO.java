package com.example.course_server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// QuizWithQuestionsDTO.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizWithQuestionsDTO {
    private Integer id;
    private String title;
    private List<QuestionWithOptionsDTO> questions;
}


package com.example.course_server.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionAnswerResponseDTO {
    private Integer questionId;
    private String questionText;
    private Integer selectedOptionId;
    private String selectedOptionText;
    private boolean correct;
}
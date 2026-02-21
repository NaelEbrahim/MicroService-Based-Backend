package com.example.course_server.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizSubmissionDTO {
    private Integer quizId;
    private List<Answer> answers;

    @Data
    @Builder
    public static class Answer {
        private Integer questionId;
        private Integer selectedOptionId;
    }
}

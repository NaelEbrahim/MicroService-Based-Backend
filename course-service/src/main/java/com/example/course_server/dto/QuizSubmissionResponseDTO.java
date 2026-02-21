package com.example.course_server.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizSubmissionResponseDTO {
    private Integer submissionId;
    private Integer quizId;
    private Integer userId;
    private LocalDateTime submittedAt;
    private int correctAnswers;
    private int totalQuestions;
    private String result;
    private double percentage;
    private List<QuestionAnswerResponseDTO> answers;
}

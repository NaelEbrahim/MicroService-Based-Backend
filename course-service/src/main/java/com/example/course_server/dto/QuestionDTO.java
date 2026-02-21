package com.example.course_server.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDTO {
    private Integer quizId;
    private String text;
    private List<OptionDTO> options = new ArrayList<>();

}

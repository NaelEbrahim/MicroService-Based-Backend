package com.example.course_server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// QuestionWithOptionsDTO.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWithOptionsDTO {
    private Integer id;
    private String text;
    private List<OptionDTO> options = new ArrayList<>();
}
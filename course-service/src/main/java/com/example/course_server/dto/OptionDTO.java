package com.example.course_server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// OptionDTO.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OptionDTO {
    private Integer id;
    private String text;
    private Boolean correct;
}
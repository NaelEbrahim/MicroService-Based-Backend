package com.example.course_server.controller;

import com.example.course_server.dto.*;
import com.example.course_server.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/courses/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/create")
    public ResponseEntity<?> createQuiz( @RequestBody QuizDTO dto) {
        return quizService.createQuiz(dto);
    }

    @PostMapping("/questions")
    public ResponseEntity<?> addQuestion(@RequestBody QuestionDTO dto) {
        return quizService.addQuestion(dto);
    }

    @GetMapping("/get/{courseId}")
    public ResponseEntity<?> getQuizForCourse(@PathVariable Integer courseId) {
        return quizService.getQuizByCourse(courseId);
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<?> submitQuiz( @RequestBody QuizSubmissionDTO dto) {
        return quizService.submitQuiz(dto);
    }

    @GetMapping("/submissions/me")
    public ResponseEntity<?> getMySubmission() {
        return quizService.getMyLatestSubmission();
    }
    @GetMapping("/submissions/{userId}")
    public ResponseEntity<?> getMySubmissionByUserId(@PathVariable Integer userId) {
        return quizService.getSubmissionByUserID(userId);
    }
}

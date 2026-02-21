package com.example.course_server.service;

import com.example.course_server.client.EnrollmentClient;
import com.example.course_server.client.UserClient;
import com.example.course_server.config.ResourceNotFoundException;
import com.example.course_server.dto.*;
import com.example.course_server.model.*;
import com.example.course_server.repository.*;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final JwtService jwtService;
    private final UserClient userClient;
    private final CourseRepository courseRepository;
    private final EnrollmentClient enrollmentClient;

    public Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        return Math.toIntExact(Long.parseLong((jwtService.extractUserId(token))));
    }

    private UserResponse fetchVerifiedUser(Integer userId) {
        try {
            ResponseEntity<UserResponse> response = userClient.getUserById(userId);
            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                return response.getBody();
            }

            if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                log.warn("User not found for ID: {}", userId);
                return new UserResponse(0, "UNKNOWN","N/A", "N/A");  // Return a dummy course
            }
            log.warn("Failed to fetch user - UserID: {}, Status: {}", userId, response.getStatusCode());
            return null;
        } catch (FeignException.NotFound notFound) {
            log.warn("User not found (404) for userId {}: {}", userId, notFound.contentUTF8());
            return new UserResponse(0, "UNKNOWN","N/A", "N/A"); // Handle Feign 404
        }
        catch (Exception e) {
            log.error("Trainer verification failed", e);
            return null;
        }
    }

    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<?> createQuiz(QuizDTO dto) {

        Optional<Course> course =courseRepository.findById(dto.getCourseId());
        if (course.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Course not found"));
        }

        if (quizRepository.findByCourseId(dto.getCourseId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Quiz already exists for this course"));
        }

        Quiz quiz = Quiz.builder()
                .title(dto.getTitle())
                .course(course.get())
                .build();
        quizRepository.save(quiz);
        return buildSuccessResponse("Quiz Created Successfully");
    }

    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<?> addQuestion( QuestionDTO dto) {
        Optional<Quiz> quiz = quizRepository.findById(dto.getQuizId());
        if (quiz.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Quiz not found"));
        }
        Question question = Question.builder()
                .text(dto.getText())
                .quiz(quiz.get())
                .build();
        questionRepository.save(question);

        List<Option> options = dto.getOptions().stream().map(opt -> Option.builder()
                .text(opt.getText())
                .correct(opt.getCorrect())
                .question(question)
                .build()).collect(Collectors.toList());

        optionRepository.saveAll(options);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Question added"));
    }
    @Transactional
    public ResponseEntity<?> getQuizByCourse(Integer courseId) {
        try {
            // 1. Fetch basic quiz without relationships
            Quiz quiz = quizRepository.findBasicQuizByCourseId(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for course ID: " + courseId));

            // 2. Fetch questions with options in separate queries
            Quiz quizWithQuestions = quizRepository.findQuizWithQuestions(quiz.getId())
                    .orElse(quiz); // fallback to basic quiz if not found

            List<Question> questionsWithOptions = questionRepository.findByQuizId(quiz.getId());
            quizWithQuestions.setQuestions(questionsWithOptions);

            // 3. Convert to DTO
            QuizWithQuestionsDTO quizDto = convertToDto(quizWithQuestions);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "quiz", quizDto
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            System.out.println("Failed to fetch quiz for course {}: {}"+ courseId+ e.getMessage()+ e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to retrieve quiz",
                            "details", e.getMessage()
                    ));
        }
    }

    private QuizWithQuestionsDTO convertToDto(Quiz quiz) {
        QuizWithQuestionsDTO dto = new QuizWithQuestionsDTO();
        dto.setId(quiz.getId());
        dto.setTitle(quiz.getTitle());

        // Safe null check for questions
        if (quiz.getQuestions() != null) {
            dto.setQuestions(quiz.getQuestions().stream()
                    .map(this::convertQuestionToDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setQuestions(Collections.emptyList());
        }

        return dto;
    }

    private QuestionWithOptionsDTO convertQuestionToDto(Question question) {
        QuestionWithOptionsDTO dto = new QuestionWithOptionsDTO();
        dto.setId(question.getId());
        dto.setText(question.getText());

        // Safe null check for options
        if (question.getOptions() != null) {
            dto.setOptions(question.getOptions().stream()
                    .map(this::convertOptionToDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setOptions(Collections.emptyList());
        }

        return dto;
    }

    private OptionDTO convertOptionToDto(Option option) {
        return new OptionDTO(
                option.getId(),
                option.getText(),
                null
        );
    }
    @PreAuthorize("hasRole('LEARNER')")
    @Transactional
    public ResponseEntity<?> submitQuiz(QuizSubmissionDTO dto) {
        try {
            // 1. Extract user info from JWT
            Integer userId = getCurrentUserId();

            UserResponse user = fetchVerifiedUser(userId);
            if (user == null) {
                return buildServiceUnavailableResponse();
            }

            if (Boolean.TRUE.equals(user.getId() == 0)) {
                // Means user was not found (graceful 404)
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found", "User ID: " + userId);
            }


            // 2. Validate quiz exists
            Quiz quiz = getQuizOrThrow(dto.getQuizId());

            // 3. Check enrollment (returns boolean)

            ResponseEntity<?> enrollmentResponse = isUserEnrolled(userId, quiz.getCourse().getId());

            if (enrollmentResponse.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                return buildEnrollmentsServiceUnavailableResponse();
            }

            Boolean enrolled = (Boolean) enrollmentResponse.getBody();

            if (enrolled == null || !enrolled) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "You Are Not Enrolled Your Course ...");
            }

            // 4. Check for previous submissions
            if (hasExistingSubmission(userId, dto.getQuizId())) {
                return conflictResponse("You have already submitted this quiz");
            }

            // 5. Process and save submission
            QuizSubmission submission = createQuizSubmission(dto, quiz, userId);

            // 6. Calculate results
            QuizResult result = calculateQuizResult(dto.getAnswers());

            // 7. Update enrollment status
            updateEnrollmentStatus(userId, quiz.getCourse().getId(), result.passed());

            // 8. Return successful response
            return okResponse(result);

        } catch (ResourceNotFoundException e) {
            return notFoundResponse(e.getMessage());
        } catch (Exception e) {
            System.out.println("Quiz submission failed"+ e);
            return errorResponse("Failed to submit quiz: " + e.getMessage());
        }
    }

    // Helper methods

    private ResponseEntity<Boolean> isUserEnrolled(Integer userId, Integer courseId) {
        try {
            return enrollmentClient.getEnrollmentStatus(userId, courseId);
        }
        catch (Exception e) {
            log.error("Enrollment service is unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
        }
    }


    private Quiz getQuizOrThrow(Integer quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
    }

    private boolean hasExistingSubmission(Integer userId, Integer quizId) {
        return submissionRepository.existsByUserIdAndQuizId(userId, quizId);
    }

    private QuizSubmission createQuizSubmission(QuizSubmissionDTO dto, Quiz quiz, Integer userId) {
        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setUserId(userId);
        submission.setSubmittedAt(LocalDateTime.now());

        List<QuestionAnswer> answers = dto.getAnswers().stream()
                .map(answer -> createQuestionAnswer(answer, submission))
                .collect(Collectors.toList());

        submission.setAnswers(answers);
        return submissionRepository.save(submission);
    }

    private QuestionAnswer createQuestionAnswer(QuizSubmissionDTO.Answer answer, QuizSubmission submission) {
        Question question = questionRepository.findById(answer.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        Option option = optionRepository.findById(answer.getSelectedOptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Option not found"));

        QuestionAnswer qa = new QuestionAnswer();
        qa.setSubmission(submission);
        qa.setQuestion(question);
        qa.setSelectedOption(option);
        return qa;
    }

    private QuizResult calculateQuizResult(List<QuizSubmissionDTO.Answer> answers) {
        int correct = (int) answers.stream()
                .map(answer -> optionRepository.findById(answer.getSelectedOptionId()).orElseThrow())
                .filter(Option::isCorrect)
                .count();

        int total = answers.size();
        double percentage = total > 0 ? (correct * 100.0 / total) : 0;
        boolean passed = percentage >= 70.0;

        return new QuizResult(correct, total, percentage, passed);
    }

    private void updateEnrollmentStatus(Integer userId, Integer courseId, boolean passed) {
        try {
            enrollmentClient.updateEnrollmentStatus(
                    new EnrollmentClient.EnrollmentStatusUpdateDTO(userId, courseId, passed ? "PASSED" : "FAILED")
            );
        } catch (Exception e) {
            System.out.println("Enrollment status update failed"+ e);
        }
    }

    // Response helpers
    private ResponseEntity<?> okResponse(QuizResult result) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "correctAnswers", result.correct(),
                "totalQuestions", result.total(),
                "percentage", result.percentage(),
                "result", result.passed() ? "Passed" : "Failed"
        ));
    }

    private ResponseEntity<?> forbiddenResponse(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", message));
    }

    private ResponseEntity<?> conflictResponse(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", message));
    }

    private ResponseEntity<?> notFoundResponse(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", message));
    }

    private ResponseEntity<?> errorResponse(String message) {
        return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", message));
    }

    // Record for quiz results
    private record QuizResult(int correct, int total, double percentage, boolean passed) {}
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> getSubmissionByUserID(Integer userId) {
        try {
            // 1. Extract and validate user from JWT
            if (userId == null) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid user ID format");
            }

            // 2. Fetch latest submission with optimized query
            Optional<QuizSubmission> latestSubmission = submissionRepository
                    .findTopByUserIdOrderBySubmittedAtDesc(userId);

            if (latestSubmission.isEmpty()) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "No quiz submissions found for this user");
            }

            // 3. Convert to DTO with all necessary details
            QuizSubmissionResponseDTO submissionDTO = convertToDetailedSubmissionDTO(latestSubmission.get());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "submission", submissionDTO
            ));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve submission: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LEARNER')")
    @Transactional
    public ResponseEntity<?> getMyLatestSubmission() {
        try {
            // 1. Extract and validate user from JWT
            Integer userId = extractUserIdFromAuthentication();
            if (userId == null) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid user ID format");
            }

            // 2. Fetch latest submission with optimized query
            Optional<QuizSubmission> latestSubmission = submissionRepository
                    .findTopByUserIdOrderBySubmittedAtDesc(userId);

            if (latestSubmission.isEmpty()) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "No quiz submissions found for this user");
            }

            // 3. Convert to DTO with all necessary details
            QuizSubmissionResponseDTO submissionDTO = convertToDetailedSubmissionDTO(latestSubmission.get());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "submission", submissionDTO
            ));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve submission: " , e.getMessage());
        }
    }

    // Helper method to extract user ID
    private Integer extractUserIdFromAuthentication() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = (String) auth.getCredentials();
            return Integer.valueOf(jwtService.extractUserId(token));
        } catch (NumberFormatException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Enhanced DTO conversion with more details
    private QuizSubmissionResponseDTO convertToDetailedSubmissionDTO(QuizSubmission submission) {
        List<QuestionAnswerResponseDTO> answerDTOs = submission.getAnswers().stream()
                .map(this::convertToDetailedAnswerDTO)
                .collect(Collectors.toList());

        long correctAnswers = answerDTOs.stream()
                .filter(QuestionAnswerResponseDTO::isCorrect)
                .count();

        double percentage = answerDTOs.isEmpty() ? 0 : (correctAnswers * 100.0 / answerDTOs.size());
        String result = percentage >= 70.0 ? "Passed" : "Failed"; // Assuming 70% is passing threshold

        return QuizSubmissionResponseDTO.builder()
                .submissionId(submission.getId())
                .quizId(submission.getQuiz().getId())
                .userId(submission.getUserId())
                .submittedAt(submission.getSubmittedAt())
                .correctAnswers((int) correctAnswers)
                .totalQuestions(answerDTOs.size())
                .percentage(percentage)
                .result(result) // Set the result here
                .answers(answerDTOs)
                .build();
    }

    private QuestionAnswerResponseDTO convertToDetailedAnswerDTO(QuestionAnswer answer) {
        return QuestionAnswerResponseDTO.builder()
                .questionId(answer.getQuestion().getId())
                .questionText(answer.getQuestion().getText())
                .selectedOptionId(answer.getSelectedOption().getId())
                .selectedOptionText(answer.getSelectedOption().getText())
                .correct(answer.getSelectedOption().isCorrect())
                .build();
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String message) {
        return buildSuccessResponse(message, null);
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String message, Map<String, Object> data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        if (data != null) response.putAll(data);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, message, null);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, String detail) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        if (detail != null) response.put("detail", detail);
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<Map<String, Object>> buildServiceUnavailableResponse() {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "User service unavailable",
                "All operations suspended until user service is restored");
    }

    private ResponseEntity<Map<String, Object>> buildEnrollmentsServiceUnavailableResponse() {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Enrollments service unavailable",
                "All operations suspended until Enrollments service is restored");
    }

}

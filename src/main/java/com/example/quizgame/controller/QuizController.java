package com.example.quizgame.controller;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.quiz.QuizRequest;
import com.example.quizgame.dto.quiz.QuizResponse;
import com.example.quizgame.dto.quiz.QuizSearchResponse;
import com.example.quizgame.entity.Quiz;
import com.example.quizgame.service.FavoriteQuizService;
import com.example.quizgame.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;

    // tạo bộ câu hỏi
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @RequestPart("quiz") QuizRequest quizRequest,
            @RequestPart(value = "image", required = false) MultipartFile quizImage,
            @RequestPart(value = "questionImages", required = false) List<MultipartFile> questionImages,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                quizService.createQuiz(quizRequest, quizImage, questionImages, userDetails.getUser())
        );
    }

    // Get all quizzes of user
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getAllQuizzesUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(quizService.getAllQuizzesOfUser(userDetails.getUser(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(quizService.getQuizById(id, userDetails.getUser()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getAllQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(quizService.getAllQuizzes(pageable, userDetails.getUser()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<QuizSearchResponse>>> searchQuizzes(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(quizService.searchQuizzes(topic, name, pageable));
    }

    @PostMapping("/{quizId}/clone")
    public ResponseEntity<ApiResponse<QuizSearchResponse>> cloneQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Quiz cloned = quizService.cloneQuiz(quizId, userDetails.getUser());
        return ResponseEntity.ok(
                new ApiResponse<>(QuizSearchResponse.fromQuizToQuizSearchResponse(cloned),
                        "Sao chép quiz thành công")
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable("id") Long quizId,
            @RequestBody QuizRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        QuizResponse updatedQuiz = quizService.updateQuiz(quizId, request, userDetails.getUser());

        return ResponseEntity.ok(
                new ApiResponse<>(updatedQuiz, "Cập nhật quiz thành công")
        );
    }


}

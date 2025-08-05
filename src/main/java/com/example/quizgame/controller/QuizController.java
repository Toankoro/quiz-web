package com.example.quizgame.controller;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.request.QuizRequest;
import com.example.quizgame.dto.response.QuizResponse;
import com.example.quizgame.reponsitory.QuizRepository;
import com.example.quizgame.service.FavoriteQuizService;
import com.example.quizgame.service.QuizService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuizController {
    private final FavoriteQuizService favoriteQuizService;
    private final QuizService quizService;

    public QuizController(FavoriteQuizService favoriteQuizService, QuizService quizService) {
        this.favoriteQuizService = favoriteQuizService;
        this.quizService = quizService;
    }
    // Tạo bộ câu hỏi
    @PostMapping
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(@RequestBody QuizRequest quizRequest,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(quizService.createQuiz(quizRequest, userDetails.getUser()));
    }
    // Get all quizzes of user
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getAllQuizzesUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(quizService.getAllQuizzesOfUser(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getAllQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(quizService.getAllQuizzes(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> searchQuizzes(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(quizService.searchQuizzes(topic, name, pageable));
    }
}

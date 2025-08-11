package com.example.quizgame.controller;

import com.example.quizgame.dto.ApiResponse;
import com.example.quizgame.dto.chat.CustomUserDetails;
import com.example.quizgame.dto.quiz.QuizSearchResponse;
import com.example.quizgame.service.FavoriteQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteQuizController {

    private final FavoriteQuizService favoriteQuizService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<String>> addToFavorites(@RequestParam("quizId") Long quizId) {
        return ResponseEntity.ok(favoriteQuizService.addToFavorites(quizId));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<String>> removeFromFavorites(@RequestParam ("quizId") Long quizId) {
        return ResponseEntity.ok(favoriteQuizService.removeFromFavorites(quizId));

    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<QuizSearchResponse>>> getUserFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(favoriteQuizService.getUserFavorites(pageable, userDetails.getUser()));
    }

}
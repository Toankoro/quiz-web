package com.example.quizgame.controller;

import com.example.quizgame.dto.response.FavoriteQuizResponse;
import com.example.quizgame.service.FavoriteQuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/favorites")
public class FavoriteQuizController {

    private final FavoriteQuizService favoriteQuizService;

    public FavoriteQuizController(FavoriteQuizService favoriteQuizService) {
        this.favoriteQuizService = favoriteQuizService;
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToFavorites(@RequestParam("quizId") Long quizId) {
        boolean success = favoriteQuizService.addToFavorites(quizId);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bộ câu hỏi được thêm vào danh sách yêu thích thành công"));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Bộ câu hỏi đã ở trong danh sách yêu thích"));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeFromFavorites(@RequestParam ("quizId") Long quizId) {
        boolean success = favoriteQuizService.removeFromFavorites(quizId);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bộ câu hỏi được xóa thành công"));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Bộ câu hỏi không nằm trong danh sách yêu thích"));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<FavoriteQuizResponse>> getUserFavorites() {
        List<FavoriteQuizResponse> favorites = favoriteQuizService.getUserFavorites();
        return ResponseEntity.ok(favorites);
    }

    // test
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FavoriteQuizResponse>> getFavoritesByUserId(@PathVariable Long userId) {
        List<FavoriteQuizResponse> favorites = favoriteQuizService.getFavoritesByUserId(userId);
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(@RequestParam Long quizId) {
        boolean isFavorite = favoriteQuizService.isFavorite(quizId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@RequestParam ("quizId") Long quizId) {
        boolean isCurrentlyFavorite = favoriteQuizService.isFavorite(quizId);
        if (isCurrentlyFavorite) {
            boolean success = favoriteQuizService.removeFromFavorites(quizId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "action", "removed",
                    "message", success ? "Đã xóa bộ câu hỏi ra khỏi danh sách yêu thích" : "Thất bại xóa bộ câu hỏi ra khỏi danh sách yêu thích"));
        } else {
            boolean success = favoriteQuizService.addToFavorites(quizId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "action", "added",
                    "message", success ? "Bộ câu hỏi đã được thêm vào danh sách yêu thích" : "Thất bại thêm câu hỏi vào danh sách yêu thích"));
        }
    }
}
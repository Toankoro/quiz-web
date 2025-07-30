package com.example.quizgame.controller;

import com.example.quizgame.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizPlayerController {

    private final QuestionService questionService;
    @PostMapping("/rooms/{roomCode}/sessions/{sessionId}/submit")
    public ResponseEntity<?> submitAllAnswers(
            @PathVariable String roomCode,
            @PathVariable String sessionId) {
        questionService.submitAllAnswers(roomCode, sessionId);
        return ResponseEntity.ok("Lưu kết quả thành công");
    }
}

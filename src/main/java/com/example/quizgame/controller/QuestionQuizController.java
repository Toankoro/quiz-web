package com.example.quizgame.controller;

import com.example.quizgame.dto.question.QuestionRequest;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.service.QuestionQuizService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class QuestionQuizController {
    @Autowired
    private QuestionQuizService questionQuizService;

    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(@RequestBody @Valid QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionQuizService.createQuestion(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @RequestBody @Valid QuestionRequest request) {
        return ResponseEntity.ok(questionQuizService.updateQuestion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionQuizService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}

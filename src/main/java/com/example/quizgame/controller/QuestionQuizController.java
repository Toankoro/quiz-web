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

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("api/quiz/question")
public class QuestionQuizController {
    @Autowired
    private QuestionQuizService questionQuizService;
    // create, update and delete question into quiz (tạo, sửa và xóa câu hỏi trong bộ câu hỏi)
    @PostMapping
    public ResponseEntity<List<QuestionResponse>> createQuestions(
            @RequestBody @Valid List<QuestionRequest> requests) {
        List<QuestionResponse> responses = requests.stream()
                .map(questionQuizService::createQuestion)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
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

package com.example.quizgame.controller;

import com.example.quizgame.dto.question.QuestionRequest;
import com.example.quizgame.dto.question.QuestionResponse;
import com.example.quizgame.service.QuestionQuizService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("api/quiz/question")
public class QuestionQuizController {
    @Autowired
    private QuestionQuizService questionQuizService;
    // create, update and delete question into quiz (tạo, sửa và xóa câu hỏi trong bộ câu hỏi)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<QuestionResponse>> createQuestions(
            @RequestPart("requests") List<QuestionRequest> requests,
            @RequestPart(value = "questionImages", required = false) List<MultipartFile> questionImages) {

        List<QuestionResponse> responses = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            MultipartFile image = (questionImages != null && i < questionImages.size())
                    ? questionImages.get(i)
                    : null;
            responses.add(questionQuizService.createQuestion(requests.get(i), image));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @RequestPart("request") @Valid QuestionRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(questionQuizService.updateQuestion(id, request, image));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionQuizService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}

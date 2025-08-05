package com.example.quizgame.controller;

import com.example.quizgame.dto.answer.AnswerMessage;
import com.example.quizgame.dto.response.AnswerResult;
import com.example.quizgame.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class QuestionController {
    private QuestionService questionService;
    private final SimpMessagingTemplate messagingTemplate;


    @PostMapping("/room/{pinCode}/submit-answer")
    public ResponseEntity<AnswerResult> submitAnswer(
            @PathVariable String pinCode,
            @RequestParam String clientSessionId,
            @RequestBody AnswerMessage message,
            Principal principal) {
        String username = principal.getName();
        AnswerResult result = questionService.handleAnswer(pinCode, username, message);
        messagingTemplate.convertAndSendToUser(
                clientSessionId,
                "/queue/answer-result",
                result
        );
        return ResponseEntity.ok(result);
    }


}

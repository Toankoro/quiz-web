package com.example.quizgame.dto;

import lombok.Data;

@Data
public class AnswerMessage {
    private Long questionId;
    private String selectedAnswer;
    private String sessionId;
    private String clientSessionId;
    private Long timeTaken;
}

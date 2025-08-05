package com.example.quizgame.dto.request;

import lombok.Data;

@Data
public class PlayerAnswerRequest {
    private Long playerId;
    private Long questionId;
    private String selectedAnswer;
    private String sessionId;
    private long timeTaken;
}

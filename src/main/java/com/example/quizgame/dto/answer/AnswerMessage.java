package com.example.quizgame.dto.answer;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerMessage {
    private String selectedAnswer;
    private Long timeTaken;
    private String clientSessionId;
}

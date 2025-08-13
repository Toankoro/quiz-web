package com.example.quizgame.dto.answer;

import lombok.Data;

@Data
public class AnswerMessage {
    private String selectedAnswer;
    private Float timeTaken;
    private String clientSessionId;
}

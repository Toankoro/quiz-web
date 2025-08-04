package com.example.quizgame.dto;

import lombok.Data;

@Data
public class AnswerMessage {
    private String selectedAnswer;
    private Long timeTaken;
}

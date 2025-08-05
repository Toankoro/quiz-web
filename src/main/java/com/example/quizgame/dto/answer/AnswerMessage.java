package com.example.quizgame.dto.answer;

import lombok.Data;

@Data
public class AnswerMessage {
    private String selectedAnswer;
    private Long timeTaken;
}

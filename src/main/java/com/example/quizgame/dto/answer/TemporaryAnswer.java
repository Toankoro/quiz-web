package com.example.quizgame.dto.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryAnswer {
    private Long questionId;
    private String selectedAnswer;
    private boolean isCorrect;
    private int score;
    private Long timeTaken;
}
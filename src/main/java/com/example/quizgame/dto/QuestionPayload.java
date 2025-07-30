package com.example.quizgame.dto;

import lombok.Data;

@Data
public class QuestionPayload {
    private String content;
    private String answerA;
    private String answerB;
    private String answerC;
    private String answerD;
    int timeLimit;
    private int questionIndex;
    private int totalQuestions;
}
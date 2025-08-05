package com.example.quizgame.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReconnectResponse {
    private boolean success;
    private String message;
    private QuestionResponse currentQuestion;
    private Integer currentScore;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private boolean hasAnsweredCurrentQuestion;
    private AnswerResult lastAnswerResult;
}
package com.example.quizgame.dto.answer;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryDetailDTO {
    private Long questionId;
    private String questionContent;
    private String answerA;
    private String answerB;
    private String answerC;
    private String answerD;
    private String selectedAnswer;
    private boolean correct;
    private Integer score;
    private String correctAnswer;
    private String explanation;
}

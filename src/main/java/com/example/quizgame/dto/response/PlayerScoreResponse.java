package com.example.quizgame.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerScoreResponse {
    private String playerName;
    private String clientSessionId;
    private Integer totalScore;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Double accuracy;
}
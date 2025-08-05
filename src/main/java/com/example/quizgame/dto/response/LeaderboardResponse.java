package com.example.quizgame.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaderboardResponse {
    private String roomCode;
    private List<PlayerScoreResponse> players;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
}
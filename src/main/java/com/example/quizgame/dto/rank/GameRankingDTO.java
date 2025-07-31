package com.example.quizgame.dto.rank;

import com.example.quizgame.entity.GameRanking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameRankingDTO {
    private Long id;
    private String firstName;
    private int score;
    private int correctCount;
    private int ranking;

    public static GameRankingDTO from(GameRanking r) {
        return new GameRankingDTO(
                r.getUser().getId(),
                r.getUser().getFirstname(),
                r.getScore(),
                r.getCorrectCount(),
                r.getRanking()
        );
    }
}


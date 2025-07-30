package com.example.quizgame.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AnswerResult {
    private String sessionId;
    private boolean correct;
    private String playerName;
    private int score;
}
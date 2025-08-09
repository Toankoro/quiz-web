package com.example.quizgame.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconnectResponse {
    private String clientSessionId;
    private Long currentQuestionId;
    private String questionContent;
    private List<String> answers;
    private long startTime;
    private int timeLimit;
    private boolean hasAnswered;

    public static ReconnectResponse noCurrentQuestion(String clientSessionId) {
        return new ReconnectResponse(
                clientSessionId,
                null,
                null,
                null,
                0L,
                0,
                false
        );
    }
}


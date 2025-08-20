package com.example.quizgame.dto.question;

import lombok.*;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Getter
@Setter
public class ReconnectResponse {
    private Long questionId;
    private QuestionResponse currentQuestion;
    private long remainingTime;
    private boolean alreadyAnswered;
    private Set<String> availableCards;

    public ReconnectResponse(Long questionId,
                             QuestionResponse currentQuestion,
                             long remainingTime,
                             boolean alreadyAnswered,
                             Set<String> availableCards) {
        this.questionId = questionId;
        this.currentQuestion = currentQuestion;
        this.remainingTime = remainingTime;
        this.alreadyAnswered = alreadyAnswered;
        this.availableCards = availableCards;
    }
}


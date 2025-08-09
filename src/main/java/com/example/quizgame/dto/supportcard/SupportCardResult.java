package com.example.quizgame.dto.supportcard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SupportCardResult {
    private String clientSessionId;
    private Long questionId;
    private SupportCardType cardType;
    private String message;
    private Object effectData;

    public SupportCardResult (Long questionId, SupportCardType cardType, String clientSessionId) {
        this.questionId = questionId;
        this.cardType = cardType;
        this.clientSessionId = clientSessionId;
    }
}

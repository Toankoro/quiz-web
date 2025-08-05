package com.example.quizgame.dto.supportcard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportCardResult {
    private Long questionId;
    private String sessionId;
    private SupportCardType type;
    private String message;
    private Object effectData;
}

package com.example.quizgame.dto.supportcard;

import lombok.Data;

@Data
public class SupportCardMessage {
    private String sessionId;
    private String clientSessionId;
    private Long questionId;
    private SupportCardType cardType;
}

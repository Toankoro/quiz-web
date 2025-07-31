package com.example.quizgame.dto;

import lombok.Data;

@Data
public class SupportCardMessage {
    private String sessionId;
    private String clientSessionId;
    private Long questionId;
    private SupportCardType cardType;
}

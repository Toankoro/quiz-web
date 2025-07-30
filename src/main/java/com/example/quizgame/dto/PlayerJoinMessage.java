package com.example.quizgame.dto;

import lombok.Data;

@Data
public class PlayerJoinMessage {
    private String playerName;
    private String roomCode;
    private String clientSessionId;
}
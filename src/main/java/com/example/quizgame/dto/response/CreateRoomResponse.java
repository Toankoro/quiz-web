package com.example.quizgame.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateRoomResponse {
    private String hostUsername;
    private String roomCode;
    private String hostClientSessionId;
}

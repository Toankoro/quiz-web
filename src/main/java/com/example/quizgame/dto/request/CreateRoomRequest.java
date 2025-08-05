package com.example.quizgame.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {
    private String hostUsername;
    private Long quizId;
    private String hostClientSessionId;
}

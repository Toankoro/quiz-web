package com.example.quizgame.dto.response;

import com.example.quizgame.dto.RoomState;
import lombok.Data;

@Data
public class RoomDTO {
    private String roomId;
    private String hostName;
    private RoomState state;
}
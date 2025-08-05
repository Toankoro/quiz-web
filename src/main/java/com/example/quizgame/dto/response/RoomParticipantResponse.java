package com.example.quizgame.dto.response;

import com.example.quizgame.entity.RoomParticipant;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RoomParticipantResponse {
    private String name;
    private String pinCode;

    public static RoomParticipantResponse fromRoomParticipantToResponse (RoomParticipant roomParticipant) {
        return RoomParticipantResponse.builder()
                .name(roomParticipant.getUser().getUsername())
                .pinCode(roomParticipant.getRoom().getPinCode())
                .build();
    }

}

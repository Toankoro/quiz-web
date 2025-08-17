package com.example.quizgame.dto.room;

import com.example.quizgame.entity.Room;
import com.example.quizgame.entity.RoomParticipant;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomJoinResponse {
    private Long roomId;
    private String pinCode;
    private String qrCodeUrl;
    private String quizTitle;
    private String clientSessionId;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private boolean isStarted;
    List<ParticipantDTO> participants;

    public static RoomJoinResponse from(Room room, String clientSessionId, List<ParticipantDTO> participants) {
        return RoomJoinResponse.builder()
                .roomId(room.getId())
                .pinCode(room.getPinCode())
                .qrCodeUrl(room.getQrCodeUrl())
                .quizTitle(room.getQuiz().getTopic())
                .clientSessionId(clientSessionId)
                .createdAt(room.getCreatedAt())
                .startedAt(room.getStartedAt())
                .endedAt(room.getEndedAt())
                .isStarted(room.getStartedAt() != null)
                .participants(participants)
                .build();
    }
}
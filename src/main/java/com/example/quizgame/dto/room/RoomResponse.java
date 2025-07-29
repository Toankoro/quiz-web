package com.example.quizgame.dto.room;

import com.example.quizgame.entity.Room;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long roomId;
    private String pinCode;
    private String qrCodeUrl;
    private String quizTitle;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private boolean isStarted;

    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .roomId(room.getId())
                .pinCode(room.getPinCode())
                .qrCodeUrl(room.getQrCodeUrl())
                .quizTitle(room.getQuiz().getTitle())
                .createdAt(room.getCreatedAt())
                .startedAt(room.getStartedAt())
                .endedAt(room.getEndedAt())
                .isStarted(room.getStartedAt() != null)
                .build();
    }
}
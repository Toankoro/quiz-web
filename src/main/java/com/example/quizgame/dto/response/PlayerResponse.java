package com.example.quizgame.dto.response;

import com.example.quizgame.entity.Player;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerResponse {
    private String name;
    private String sessionId;
    private String clientSessionId;
    private String roomCode;


    public static PlayerResponse fromPlayerToPlayerResponse (Player player) {
        return PlayerResponse.builder()
                .name(player.getName())
                .sessionId(player.getSessionId())
                .clientSessionId(player.getClientSessionId())
                .build();
    }
}

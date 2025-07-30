package com.example.quizgame.websocket;

import com.example.quizgame.dto.response.PlayerResponse;
import com.example.quizgame.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Optional;

@Component
public class WebSocketEventListener {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        System.out.println("[DISCONNECT] SessionId: " + sessionId);

        // Xóa người chơi dựa vào sessionId
        Optional<String> roomCodeOptional = playerService.removePlayerBySessionId(sessionId);

        roomCodeOptional.ifPresent(roomCode -> {
            List<PlayerResponse> updatedPlayers = playerService.getPlayersInRoom(roomCode);
            System.out.println("[SEND] Updated player list to /topic/room/" + roomCode + "/players: " + updatedPlayers);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/players", updatedPlayers);
        });
    }
}

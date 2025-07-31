package com.example.quizgame.controller;

import com.example.quizgame.dto.AnswerMessage;
import com.example.quizgame.dto.PlayerJoinMessage;
import com.example.quizgame.dto.ReconnectMessage;
import com.example.quizgame.dto.response.PlayerResponse;
import com.example.quizgame.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin("*")
@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final PlayerService playerService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomCode}/join")
    public void joinRoom(@DestinationVariable String roomCode,
            PlayerJoinMessage message,
            SimpMessageHeaderAccessor accessor) {
        String wsSessionId = accessor.getSessionId();
        String clientSessionId = message.getClientSessionId();
        String playerName = message.getPlayerName();

        System.out.println("[JOIN] Player: " + playerName + ", clientSessionId: " + clientSessionId + ", wsSessionId: "
                + wsSessionId + ", room: " + roomCode);

        playerService.registerPlayer(playerName, roomCode, wsSessionId, clientSessionId);

        List<PlayerResponse> players = playerService.getPlayersInRoom(roomCode);
        System.out.println("[SEND] Updated player list to /topic/room/" + roomCode + "/players: " + players);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/players", players);
    }



    @MessageMapping("/room/{roomCode}/answer")
    public void answer(@DestinationVariable String roomCode,
            AnswerMessage message,
            SimpMessageHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        message.setSessionId(sessionId);
        playerService.saveAnswer(message);
    }

    @MessageMapping("/reconnect")
    public void reconnect(@Payload ReconnectMessage message, SimpMessageHeaderAccessor accessor) {
        String clientSessionId = message.getClientSessionId();
        String newSessionId = accessor.getSessionId();
        String roomCode = message.getRoomCode();

        boolean success = playerService.reconnectPlayer(roomCode, clientSessionId, newSessionId);

        if (success) {
            System.out.println("Reconnect thành công");
        } else {
            messagingTemplate.convertAndSendToUser(
                    newSessionId,
                    "/queue/errors",
                    "Reconnect thất bại. Không tìm thấy session cũ.");
            return;
        }
        List<PlayerResponse> players = playerService.getPlayersInRoom(roomCode);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/players", players);
    }

}

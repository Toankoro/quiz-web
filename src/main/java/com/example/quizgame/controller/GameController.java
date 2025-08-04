package com.example.quizgame.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;

    public GameController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Endpoint để join game
    @MessageMapping("/game/join")
    public void joinGame(@Payload Map<String, Object> payload, Principal principal) {
        String username = principal.getName();
        System.out.println("User joined game: " + username);

        // Gửi message xác nhận đến chính user đó
        Map<String, Object> response = new HashMap<>();
        response.put("type", "JOIN_SUCCESS");
        response.put("message", "Bạn đã join game thành công!");
        response.put("username", username);
        response.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", response);

        // Broadcast đến tất cả người chơi khác
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "PLAYER_JOINED");
        broadcast.put("username", username);
        broadcast.put("message", username + " đã tham gia game!");

        messagingTemplate.convertAndSend("/topic/game/updates", broadcast);
    }

    @MessageMapping("/test/send")
    public void sendTestMessage(@Payload Map<String, Object> payload, Principal principal) {
        String fromUser = principal.getName();
        String toUser = (String) payload.get("toUser");
        String message = (String) payload.get("message");

        if (toUser != null && message != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "PRIVATE_MESSAGE");
            response.put("from", fromUser);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());

            // Gửi đến user cụ thể
            messagingTemplate.convertAndSendToUser(toUser, "/queue/messages", response);

            // Gửi confirmation lại cho người gửi
            Map<String, Object> confirmation = new HashMap<>();
            confirmation.put("type", "MESSAGE_SENT");
            confirmation.put("to", toUser);
            confirmation.put("message", "Message đã được gửi thành công!");

            messagingTemplate.convertAndSendToUser(fromUser, "/queue/notifications", confirmation);
        }
    }
}
package com.example.quizgame.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/ws")
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send")
    public ResponseEntity<String> sendToUser(@RequestParam String clientSessionId,
                                             @RequestBody String message) {
        messagingTemplate.convertAndSendToUser(clientSessionId, "/queue/notify", message);
        return ResponseEntity.ok("Message sent to " + clientSessionId);
    }
}

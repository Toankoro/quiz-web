package com.example.quizgame.controller;

import com.example.quizgame.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final GameService gameService;

    public TestController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/send-notification")
    public ResponseEntity<String> sendNotification(
            @RequestParam String username,
            @RequestParam String message) {

        gameService.sendNotificationToUser(username, message);
        return ResponseEntity.ok("Notification sent to " + username);
    }
}
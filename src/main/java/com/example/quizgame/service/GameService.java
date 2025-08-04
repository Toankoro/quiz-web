package com.example.quizgame.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GameService {

    private final SimpMessagingTemplate messagingTemplate;

    public GameService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Method để gửi thông báo đến user cụ thể
    public void sendNotificationToUser(String username, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NOTIFICATION");
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }

    // Method để gửi câu hỏi Kahoot đến user
    public void sendQuestionToUser(String username, Object question) {
        messagingTemplate.convertAndSendToUser(username, "/queue/questions", question);
    }

    // Method để gửi kết quả đến user
    public void sendResultToUser(String username, Object result) {
        messagingTemplate.convertAndSendToUser(username, "/queue/results", result);
    }
}
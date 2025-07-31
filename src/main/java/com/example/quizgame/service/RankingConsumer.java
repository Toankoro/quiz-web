package com.example.quizgame.service;

import com.example.quizgame.dto.rank.GameRankingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RankingConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    public RankingConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "room-rankings", groupId = "quiz-group")
    public void handleRankingUpdate(@Payload List<GameRankingDTO> rankingDTOs,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String roomId) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/ranking", rankingDTOs);
    }
}

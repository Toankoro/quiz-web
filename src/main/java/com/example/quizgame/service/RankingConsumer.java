package com.example.quizgame.service;

import com.example.quizgame.dto.rank.GameRankingDTO;
import com.example.quizgame.entity.Room;
import com.example.quizgame.reponsitory.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;

    @KafkaListener(topics = "room-rankings", groupId = "quiz-group")
    public void handleRankingUpdate(@Payload List<GameRankingDTO> rankingDTOs,
            @Header(KafkaHeaders.RECEIVED_KEY) String roomId) {
        try {
            Long roomIdLong = Long.parseLong(roomId);
            Room room = roomRepository.findById(roomIdLong).orElse(null);
            if (room != null) {
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/ranking", rankingDTOs);
            }
        } catch (NumberFormatException e) {
            // Log error but don't crash
            System.err.println("Invalid roomId format in Kafka message: " + roomId);
        }
    }
}

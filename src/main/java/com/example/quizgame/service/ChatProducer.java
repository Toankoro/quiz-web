package com.example.quizgame.service;

import com.example.quizgame.dto.chat.ChatMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatProducer {
    @Autowired
    @Qualifier("chatMessageDTOKafkaTemplate")
    private KafkaTemplate<String,ChatMessageDTO > kafkaTemplate;

    public void sendMessage(ChatMessageDTO message) {
        kafkaTemplate.send("chat-topic", message);
    }
}
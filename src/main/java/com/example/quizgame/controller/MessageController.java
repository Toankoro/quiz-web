package com.example.quizgame.controller;

import com.example.quizgame.kafka.KafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/v1/kafka")
public class MessageController {
    private KafkaProducer kafkaProducer;

    public MessageController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }
    //http://localhost:8080/api/v1/kafka?message=tung nguyen
    @GetMapping("/publish")
    public ResponseEntity<String> publish (@RequestParam("message") String message){
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message sent to topic");
    }
}

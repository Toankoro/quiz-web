package com.example.quizgame.controller;

import com.example.quizgame.kafka.JsonKafkaProducer;
import com.example.quizgame.payload.MessageKafka;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/kafka")
public class JsonMessageController {
    private JsonKafkaProducer jsonKafkaProducer;

    public JsonMessageController(JsonKafkaProducer jsonKafkaProducer) {
        this.jsonKafkaProducer = jsonKafkaProducer;
    }

    @PostMapping(value = "publish")
    public ResponseEntity<String> publish (@RequestBody MessageKafka messageKafka) {
            jsonKafkaProducer.sendMessage(messageKafka);
            return ResponseEntity.ok("Message json sent to kafka");
    }
}

package com.example.quizgame.kafka;

import com.example.quizgame.payload.MessageKafka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MailKafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(MailKafkaConsumer.class);

    @KafkaListener(topics="kahoot_json", groupId = "group_id")
    public void consume(MessageKafka message) {
        logger.info(String.format("Json message received -> %s", message));
    }
}

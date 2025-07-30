package com.example.quizgame.kafka;

import com.example.quizgame.payload.MessageKafka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class JsonKafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private KafkaTemplate<String, MessageKafka> kafkaTemplate;

    public JsonKafkaProducer(KafkaTemplate<String, MessageKafka> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage (MessageKafka data) {
        logger.info(String.format("Send message to topic -> %s", data));
        Message<MessageKafka> message = MessageBuilder.withPayload(data).setHeader(KafkaHeaders.TOPIC, "kahoot_json").build();
        kafkaTemplate.send(message);
    }
}

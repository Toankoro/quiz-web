package com.example.quizgame.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic kahootTopic () {
        return TopicBuilder.name("send-mail").build();
    }

    @Bean
    public NewTopic kahootJsonTopic () {
        return TopicBuilder.name("send-mail-json").build();
    }

    @Bean
    public NewTopic kahootChatTopic () {
        return TopicBuilder.name("chat-topic").build();
    }
}

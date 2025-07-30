package com.example.quizgame.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic kahootTopic () {
        return TopicBuilder.name("kahoot").build();
    }

    @Bean
    public NewTopic kahootJsonTopic () {
        return TopicBuilder.name("kahoot_json").build();
    }
}

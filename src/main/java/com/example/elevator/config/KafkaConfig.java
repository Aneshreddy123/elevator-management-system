package com.example.elevator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.movement}")
    private String movementTopic;

    @Bean
    public NewTopic movementTopic() {
        return TopicBuilder.name(movementTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

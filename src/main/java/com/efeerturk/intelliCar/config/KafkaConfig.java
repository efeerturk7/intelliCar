package com.efeerturk.intelliCar.config;

import com.efeerturk.intelliCar.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic offerEventsTopic() {
        return TopicBuilder.name(KafkaTopics.OFFER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic carViewEventsTopic() {
        return TopicBuilder.name(KafkaTopics.CAR_VIEW_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic carStatusEventsTopic() {
        return TopicBuilder.name(KafkaTopics.CAR_STATUS_EVENTS).partitions(3).replicas(1).build();
    }
}

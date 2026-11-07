package com.efeerturk.intelliCar.config;

import com.efeerturk.intelliCar.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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


    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(5_000L, 3L));
    }
}

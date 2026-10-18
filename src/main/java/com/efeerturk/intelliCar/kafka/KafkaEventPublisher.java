package com.efeerturk.intelliCar.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(topic, key, event);
                }
            });
        } else {
            send(topic, key, event);
        }
    }

    private void send(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka'ya gönderilemedi. topic={}, key={}", topic, key, ex);
                } else {
                    log.debug("Kafka'ya gönderildi. topic={}, partition={}, offset={}",
                            topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Kafka gönderimi başlatılamadı. topic={}, key={}", topic, key, ex);
        }
    }
}

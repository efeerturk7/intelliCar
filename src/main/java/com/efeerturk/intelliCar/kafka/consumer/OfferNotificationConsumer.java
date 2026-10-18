package com.efeerturk.intelliCar.kafka.consumer;

import com.efeerturk.intelliCar.event.OfferEvent;
import com.efeerturk.intelliCar.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;



@Slf4j
@Component
public class OfferNotificationConsumer {

    @KafkaListener(topics = KafkaTopics.OFFER_EVENTS, groupId = "notification-group")
    public void handle(OfferEvent event) {
        switch (event.eventType()) {
            case OFFER_CREATED -> log.info(
                    "[BİLDİRİM] Satıcı {}: '{}' ilanınıza {} TL teklif geldi (offer={})",
                    event.sellerId(), event.carTitle(), event.offeredPrice(), event.offerId());
            case OFFER_RESPONDED -> log.info(
                    "[BİLDİRİM] Alıcı {}: '{}' için teklifiniz {} oldu (offer={})",
                    event.buyerId(), event.carTitle(), event.status(), event.offerId());
        }
    }
}

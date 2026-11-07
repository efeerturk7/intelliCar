package com.efeerturk.intelliCar.kafka.consumer;

import com.efeerturk.intelliCar.event.OfferEvent;
import com.efeerturk.intelliCar.kafka.KafkaTopics;
import com.efeerturk.intelliCar.notification.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Teklif event'lerini dinler ve ilgili tarafa e-posta gönderir:
 * yeni teklif gelince satıcıya, teklif yanıtlanınca alıcıya.
 */
@Component
@RequiredArgsConstructor
public class OfferNotificationConsumer {

    private final EmailNotificationService emailNotificationService;

    @KafkaListener(topics = KafkaTopics.OFFER_EVENTS, groupId = "notification-group")
    public void handle(OfferEvent event) {
        switch (event.eventType()) {
            case OFFER_CREATED -> emailNotificationService.notifySellerOfNewOffer(event);
            case OFFER_RESPONDED -> emailNotificationService.notifyBuyerOfDecision(event);
        }
    }
}

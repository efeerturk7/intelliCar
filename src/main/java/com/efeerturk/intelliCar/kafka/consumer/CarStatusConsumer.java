package com.efeerturk.intelliCar.kafka.consumer;

import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.enums.OfferStatus;
import com.efeerturk.intelliCar.event.CarStatusChangedEvent;
import com.efeerturk.intelliCar.kafka.KafkaTopics;
import com.efeerturk.intelliCar.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarStatusConsumer {

    private final OfferRepository offerRepository;

    @KafkaListener(topics = KafkaTopics.CAR_STATUS_EVENTS, groupId = "offer-expiry-group")
    @Transactional
    public void handle(CarStatusChangedEvent event) {
        if (event.newStatus() == CarStatus.SOLD || event.newStatus() == CarStatus.INACTIVE) {
            int expired = offerRepository.expirePendingOffers(event.carId(), OfferStatus.EXPIRED, OfferStatus.PENDING);
            log.info("Car {} {} oldu, {} bekleyen teklif EXPIRED yapıldı", event.carId(), event.newStatus(), expired);
        }
    }
}

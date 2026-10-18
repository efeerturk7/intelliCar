package com.efeerturk.intelliCar.kafka.consumer;

import com.efeerturk.intelliCar.event.CarViewedEvent;
import com.efeerturk.intelliCar.kafka.KafkaTopics;
import com.efeerturk.intelliCar.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;



@Component
@RequiredArgsConstructor
public class CarViewConsumer {

    private final CarRepository carRepository;

    @KafkaListener(topics = KafkaTopics.CAR_VIEW_EVENTS, groupId = "view-counter-group")
    @Transactional
    public void handle(CarViewedEvent event) {
        carRepository.incrementViewCount(event.carId());
    }
}

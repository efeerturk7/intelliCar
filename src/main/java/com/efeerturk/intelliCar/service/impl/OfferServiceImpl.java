package com.efeerturk.intelliCar.service.impl;

import com.efeerturk.intelliCar.dto.request.OfferCreateRequest;
import com.efeerturk.intelliCar.dto.response.OfferResponse;
import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.enums.OfferStatus;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import com.efeerturk.intelliCar.mapper.OfferMapper;
import com.efeerturk.intelliCar.model.Car;
import com.efeerturk.intelliCar.model.Offer;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.repository.CarRepository;
import com.efeerturk.intelliCar.repository.OfferRepository;
import com.efeerturk.intelliCar.repository.UserRepository;
import com.efeerturk.intelliCar.service.OfferService;
import com.efeerturk.intelliCar.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {
    private final OfferRepository offerRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final OfferMapper offerMapper;
    private final CarRepository carRepository;

    @Override
    @Transactional
    public OfferResponse createOffer(OfferCreateRequest request, UUID buyerId) {

        Car dbCar = carRepository.findById(request.carId())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.CAR_NOT_FOUND,request.carId().toString())));


        if (dbCar.getStatus() != CarStatus.ACTIVE) {
            log.warn("Aktif olmayan araca teklif verilmeye çalışıldı. Car ID: {}", dbCar.getId());
            throw new BaseException(new ErrorMessage(MessageType.CAR_NOT_ACTIVE, request.carId().toString()));
        }


        if (dbCar.getSeller().getId().equals(buyerId)) {
            log.warn("Kullanıcı kendi aracına teklif veremez. User ID: {}, Car ID: {}", buyerId, dbCar.getId());
            throw new BaseException(new ErrorMessage(MessageType.CANNOT_OFFER_ON_OWN_CAR,buyerId.toString()));
        }


        if (offerRepository.existsByCarIdAndBuyerIdAndStatus(dbCar.getId(), buyerId, OfferStatus.PENDING)) {
            log.warn("Bekleyen teklif varken yeni teklif verilemez. Buyer ID: {}, Car ID: {}", buyerId, dbCar.getId());
            throw new BaseException(new ErrorMessage(MessageType.PENDING_OFFER_ALREADY_EXISTS,buyerId.toString()));
        }

        User buyer = userService.getUserEntityById(buyerId);

        Offer offer = offerMapper.toEntity(request);
        offer.setCar(dbCar);
        offer.setBuyer(buyer);
        offer.setStatus(OfferStatus.PENDING);

        Offer savedOffer = offerRepository.save(offer);
        log.info("Teklif başarıyla oluşturuldu. Offer ID: {}", savedOffer.getOfferId());

        return offerMapper.toResponse(savedOffer);
    }
    @Override
    @Transactional
    public OfferResponse respondToOffer(UUID offerId, OfferStatus newStatus, UUID sellerId) {
        Offer offer = offerRepository.findById(offerId).orElseThrow(() -> new RuntimeException("Offer ID: " + offerId));
        if (!offer.getCar().getSeller().getId().equals(sellerId)) {
            log.warn("bu teklifi yanıtlama yetkiniz yok");
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_OFFER_OPERATION,offerId.toString()));
        }
            if (offer.getStatus() != OfferStatus.PENDING) {
                log.warn("yalnızca beklemede olan teklifler yanıtlanabilir");
                throw new BaseException(new ErrorMessage(MessageType.OFFER_NOT_PENDING,offerId.toString()));
            }
            if (newStatus == OfferStatus.PENDING) {
                log.warn("geçersiz teklif durumu");
                throw new BaseException(new ErrorMessage(MessageType.INVALID_OFFER_STATUS,offerId.toString()));
            }
            offer.setStatus(newStatus);
            Offer savedOffer = offerRepository.save(offer);
        log.info("Teklif {} durumu {} olarak güncellendi.", savedOffer.getOfferId(), newStatus);
            return offerMapper.toResponse(savedOffer);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponse> getOffersForCar(UUID carId, UUID sellerId, Pageable pageable){
        Car dbCar=carRepository.findById(carId).orElseThrow(() -> new RuntimeException("Car ID: " + carId));
        if (!dbCar.getSeller().getId().equals(sellerId)) {
            log.warn("bu ilana gelen teklifleri yalnızca araç sahibi görüntüleyebilir");
            throw new BaseException(new ErrorMessage(MessageType.ONLY_THE_VEHICLE_OWNER_CAN_VIEW_THE_OFFERS,carId.toString()));
        }
        return offerRepository.findByCarId(carId, pageable).map(offerMapper::toResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponse>getMyOffers(UUID buyerId,Pageable pageable){
        return offerRepository.findByBuyerId(buyerId,pageable).map(offerMapper::toResponse);
    }
}

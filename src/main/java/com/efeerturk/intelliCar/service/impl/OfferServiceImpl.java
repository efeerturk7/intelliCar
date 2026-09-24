package com.efeerturk.intelliCar.service.impl;

import com.efeerturk.intelliCar.dto.request.OfferCreateRequest;
import com.efeerturk.intelliCar.dto.response.OfferResponse;
import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.enums.OfferStatus;
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

import java.util.Optional;
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
                .orElseThrow(() -> new RuntimeException("Araç bulunamadı: " + request.carId()));


        if (dbCar.getStatus() != CarStatus.ACTIVE) {
            log.warn("Aktif olmayan araca teklif verilmeye çalışıldı. Car ID: {}", dbCar.getId());
            return null; // TODO: Sadece yayındaki araçlara teklif verilebilir hatası eklenecek
        }


        if (dbCar.getSeller().getId().equals(buyerId)) {
            log.warn("Kullanıcı kendi aracına teklif veremez. User ID: {}, Car ID: {}", buyerId, dbCar.getId());
            return null; // TODO: Kendi aracınıza teklif veremezsiniz hatası eklenecek
        }


        if (offerRepository.existsByCarIdAndBuyerIdAndStatus(dbCar.getId(), buyerId, OfferStatus.PENDING)) {
            log.warn("Bekleyen teklif varken yeni teklif verilemez. Buyer ID: {}, Car ID: {}", buyerId, dbCar.getId());
            return null; // TODO: Zaten onay bekleyen bir teklif var hatası eklenecek
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
            log.warn("bu teklifi yanıtlama yetkiniz yok");//TODO:Yetki hatası eklenecek
            return null;
        }
            if (offer.getStatus() != OfferStatus.PENDING) {
                log.warn("yalnızca beklemede olan teklifler yanıtlanabilir");
                return null;
            }
            if (newStatus == OfferStatus.PENDING) {
                log.warn("geçersiz teklif durumu");//TODO:Geçersiz teklif durumu hatası eklenecek
                return null;
            }
            offer.setStatus(newStatus);
            Offer savedOffer = offerRepository.save(offer);
        log.info("Teklif {} durumu {} olarak güncellendi.", savedOffer.getOfferId(), newStatus);
            return offerMapper.toResponse(savedOffer);
    }
    @Override
    @Transactional
    public Page<OfferResponse> getOffersForCar(UUID carId, UUID sellerId, Pageable pageable){
        Car dbCar=carRepository.findById(carId).orElseThrow(() -> new RuntimeException("Car ID: " + carId));
        if (!dbCar.getSeller().getId().equals(sellerId)) {
            log.warn("bu ilana gelen teklifleri yalnızca araç sahibi görüntüleyebilir");
            return null;
        }
        return offerRepository.findByBuyerId(carId, pageable).map(offerMapper::toResponse);
    }
    @Override
    @Transactional
    public Page<OfferResponse>getMyOffers(UUID buyerId,Pageable pageable){
        return offerRepository.findByBuyerId(buyerId,pageable).map(offerMapper::toResponse);
    }
}

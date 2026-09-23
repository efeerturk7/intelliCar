package com.efeerturk.intelliCar.model;

import com.efeerturk.intelliCar.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "offers")
@NoArgsConstructor
@Getter
@Setter
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID offerId;
    @ManyToOne(fetch = FetchType.LAZY)
    private Car car;
    @ManyToOne(fetch = FetchType.LAZY)
    private User buyer;
    private BigDecimal offeredPrice;
    @Enumerated(EnumType.STRING)
    private OfferStatus status;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

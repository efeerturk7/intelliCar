package com.efeerturk.intelliCar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URL;
import java.util.UUID;
@Entity
@Table(name = "carImages")
@NoArgsConstructor
@Getter
@Setter
public class CarImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Car car;
    private URL imageUrl;
}

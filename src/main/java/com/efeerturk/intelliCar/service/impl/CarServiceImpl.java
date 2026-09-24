package com.efeerturk.intelliCar.service.impl;


import com.efeerturk.intelliCar.mapper.CarMapper;

import com.efeerturk.intelliCar.repository.CarRepository;

import com.efeerturk.intelliCar.service.CarService;
import com.efeerturk.intelliCar.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;




@Service
@Slf4j
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final UserService userService;
    private final CarMapper carMapper;


}

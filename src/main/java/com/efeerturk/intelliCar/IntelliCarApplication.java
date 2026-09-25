package com.efeerturk.intelliCar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.efeerturk.intelliCar"})
@EntityScan(basePackages = {"com.efeerturk.intelliCar"})
@EnableJpaRepositories(basePackages = {"com.efeerturk.intelliCar"})
public class IntelliCarApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntelliCarApplication.class, args);
	}

}

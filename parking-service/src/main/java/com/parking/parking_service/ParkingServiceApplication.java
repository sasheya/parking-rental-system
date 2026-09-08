package com.parking.parking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.parking.parking_service", "com.parking.common_security"})
public class ParkingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkingServiceApplication.class, args);
	}

}

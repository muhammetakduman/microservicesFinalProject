package com.microservices.stok_service;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRabbit
@CrossOrigin
@EnableFeignClients
public class StokServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StokServiceApplication.class, args);
	}

}

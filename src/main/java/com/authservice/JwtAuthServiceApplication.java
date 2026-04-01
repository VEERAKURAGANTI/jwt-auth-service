package com.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.authservice.entity")
public class JwtAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtAuthServiceApplication.class, args);
	}

}

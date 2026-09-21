package com.argos.argos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArgosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArgosApplication.class, args);
	}
}

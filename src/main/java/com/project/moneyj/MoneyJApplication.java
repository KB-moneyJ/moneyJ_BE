package com.project.moneyj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoneyJApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyJApplication.class, args);
	}

}

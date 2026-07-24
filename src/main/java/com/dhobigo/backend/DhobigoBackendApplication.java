package com.dhobigo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // powers SubscriptionScheduler's daily recurring-pickup reminder
public class DhobigoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DhobigoBackendApplication.class, args);
    }
}

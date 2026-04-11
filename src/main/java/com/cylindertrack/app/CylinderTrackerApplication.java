package com.cylindertrack.app;

import com.cylindertrack.app.model.BillingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CylinderTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CylinderTrackerApplication.class, args);
    }

    @Bean
    CommandLineRunner seedPrices(BillingService billingService) {
        return args -> billingService.seedDefaultPricesIfEmpty();
    }
}
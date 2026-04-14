package com.cylindertrack.app;

import com.cylindertrack.app.model.BillingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CylinderTrackerApplication {

    public static void main(String[] args) {
        // Set JVM default timezone to IST before Spring context starts.
        // This affects: LocalDate.now(), new Date(), Thymeleaf #dates.format(),
        // Hibernate CreationTimestamp, and all ZoneId.systemDefault() calls.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(CylinderTrackerApplication.class, args);
    }

    @Bean
    CommandLineRunner seedPrices(BillingService billingService) {
        return args -> billingService.seedDefaultPricesIfEmpty();
    }
}

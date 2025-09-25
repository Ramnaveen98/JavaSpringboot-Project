package com.autobridge_api.servicecatalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class ServiceCatalogSeeder {

    private void ensure(ServiceOfferingRepository repo, String slug, String name,
                        String desc, BigDecimal price, Integer minutes) {
        repo.findBySlug(slug).orElseGet(() ->
                repo.save(ServiceOffering.builder()
                        .slug(slug)
                        .name(name)
                        .description(desc)
                        .basePrice(price)
                        .durationMinutes(minutes)
                        .active(true)
                        .build())
        );
    }

    @Bean
    CommandLineRunner seedServices(ServiceOfferingRepository repo) {
        return args -> {
            ensure(repo, "test-drive", "Test Drive",
                    "On-site test drive with an agent", BigDecimal.ZERO, 60);
            ensure(repo, "delivery", "Vehicle Delivery",
                    "Deliver purchased vehicle to your address", BigDecimal.ZERO, 120);
            ensure(repo, "oil-change", "Oil Change",
                    "Standard oil and filter change", new BigDecimal("39.99"), 45);
        };
    }
}

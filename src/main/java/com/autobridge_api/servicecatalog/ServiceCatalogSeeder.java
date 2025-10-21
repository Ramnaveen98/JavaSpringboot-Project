// src/main/java/com/autobridge_api/servicecatalog/ServiceCatalogSeeder.java
package com.autobridge_api.servicecatalog;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ServiceCatalogSeeder implements ApplicationRunner {

    private final ServiceOfferingRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        seed("test-drive",   "Test Drive",        BigDecimal.ZERO, 60,  true);
        seed("tire-rotation","Tire Rotation",     new BigDecimal("25.00"), 30, true);
        seed("delivery",     "Vehicle Delivery",  BigDecimal.ZERO, 120, true);
        seed("oil-change",   "Oil Change",        new BigDecimal("39.99"), 45, true);
    }

    private void seed(String slug, String name, BigDecimal price, Integer minutes, boolean active) {
        if (repo.existsBySlug(slug)) return;
        ServiceOffering s = new ServiceOffering();
        s.setSlug(slug);
        s.setName(name);
        s.setBasePrice(price);
        s.setDurationMinutes(minutes);
        s.setActive(active);
        repo.save(s);
    }
}

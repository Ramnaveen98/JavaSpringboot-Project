package com.autobridge_api.vehicles;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class VehicleCatalogSeeder {

    @Bean
    CommandLineRunner seedVehicleCatalog(
            VehicleMakeRepository makeRepo,
            VehicleModelRepository modelRepo,
            InventoryVehicleRepository invRepo
    ) {
        return args -> {
            // --- Makes ---
            var toyota = ensureMake(makeRepo, "Toyota");
            var honda  = ensureMake(makeRepo, "Honda");
            var tesla  = ensureMake(makeRepo, "Tesla");

            // --- Models ---
            var camry   = ensureModel(modelRepo, toyota, "Camry");
            var corolla = ensureModel(modelRepo, toyota, "Corolla");
            var civic   = ensureModel(modelRepo, honda,  "Civic");
            var model3  = ensureModel(modelRepo, tesla,  "Model 3");

            // --- Inventory vehicles (idempotent by VIN) ---
            ensureVehicle(invRepo, toyota, camry,   2022, "JTDBR32E720000001",
                    "Blue",  new BigDecimal("21990.00"),
                    InventoryStatus.AVAILABLE,
                    "https://picsum.photos/seed/camry/800/450",
                    "Reliable mid-size sedan, one-owner, clean history.");

            ensureVehicle(invRepo, honda, civic,    2021, "2HGFB2F59CH000002",
                    "Red",   new BigDecimal("18950.00"),
                    InventoryStatus.AVAILABLE,
                    "https://picsum.photos/seed/civic/800/450",
                    "Great fuel economy, recent service.");

            ensureVehicle(invRepo, tesla, model3,   2023, "5YJ3E1EA7KF000003",
                    "White", new BigDecimal("36990.00"),
                    InventoryStatus.AVAILABLE,
                    "https://picsum.photos/seed/model3/800/450",
                    "Long Range, Autopilot enabled.");
        };
    }

    private VehicleMake ensureMake(VehicleMakeRepository repo, String name) {
        return repo.findByNameIgnoreCase(name).orElseGet(() ->
                repo.save(VehicleMake.builder().name(name).build()));
    }

    private VehicleModel ensureModel(VehicleModelRepository repo, VehicleMake make, String name) {
        return repo.findByMakeIdAndNameIgnoreCase(make.getId(), name).orElseGet(() ->
                repo.save(VehicleModel.builder().make(make).name(name).build()));
    }

    private void ensureVehicle(InventoryVehicleRepository repo,
                               VehicleMake make,
                               VehicleModel model,
                               int year,
                               String vin,
                               String color,
                               BigDecimal price,
                               InventoryStatus status,
                               String imageUrl,
                               String description) {

        repo.findByVin(vin).orElseGet(() ->
                repo.save(InventoryVehicle.builder()
                        .make(make)
                        .model(model)
                        .year(year)
                        .vin(vin)
                        .color(color)
                        .price(price)
                        .status(status)
                        .imageUrl(imageUrl)
                        .description(description)
                        .build()
                )
        );
    }
}

package com.autobridge_api.vehicles.dto;

import com.autobridge_api.vehicles.InventoryStatus;
import java.math.BigDecimal;

public class VehiclesDtos {

    // For listing makes (e.g., Toyota, Honda)
    public record VehicleMakeDto(
            Long id,
            String name
    ) {}

    // For listing models within a make (e.g., Camry, Civic)
    public record VehicleModelDto(
            Long id,
            Long makeId,
            String name
    ) {}

    // For inventory cards on the home page
    public record InventoryVehicleDto(
            Long id,
            Long makeId,
            String makeName,
            Long modelId,
            String modelName,
            Integer year,
            String vin,
            String color,
            BigDecimal price,
            InventoryStatus status,
            String imageUrl,
            String description
    ) {}
}

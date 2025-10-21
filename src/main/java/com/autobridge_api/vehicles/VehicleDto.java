package com.autobridge_api.vehicles;

public record VehicleDto(
        Long id,
        String title,
        String brand,
        Double price,
        String imageUrl
) {}

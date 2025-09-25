package com.autobridge_api.servicecatalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ServiceDtos {


    public record ServiceOfferingDto(
            Long id,
            String slug,
            String name,
            String description,
            BigDecimal basePrice,
            Integer durationMinutes,
            boolean active
    ) {}


    public record UpsertServiceOfferingRequest(
            @NotBlank @Size(max = 64) String slug,
            @NotBlank @Size(max = 128) String name,
            String description,
            @DecimalMin("0.0") BigDecimal basePrice,
            @Min(1) @Max(1440) Integer durationMinutes,
            Boolean active
    ) {}
}

package com.autobridge_api.requests.dto;

public class RequestDtos {


    public record CreateServiceRequestRequest(
            Long serviceId,
            Long inventoryVehicleId,
            Long slotId,
            String userFirstName,
            String userLastName,
            String userEmail,
            String userPhone,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            String notes
    ) {}

    // ADD the two fields at the end: assignedAgentId, assignedAgentName
    public record ServiceRequestDto(
            Long id,
            String status,
            String createdAt,
            String updatedAt,

            Long serviceId,
            String serviceSlug,
            String serviceName,

            Long slotId,
            String startAtUtc,
            String endAtUtc,
            String startAtLocal,
            String endAtLocal,
            String timeZone,

            Long vehicleId,
            String vehicleMake,
            String vehicleModel,
            Integer vehicleYear,
            String vehicleVin,

            String userFirstName,
            String userLastName,
            String userEmail,
            String userPhone,

            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,

            String notes,
            Long assignedAgentId,
            String assignedAgentName
    ) {}
}

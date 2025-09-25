package com.autobridge_api.slots.dto;

import com.autobridge_api.slots.SlotStatus;
import com.autobridge_api.slots.SlotType;

/** View-friendly DTO with both UTC and local time strings */
public class SlotViewDtos {
    public record SlotViewDto(
            Long id,
            SlotType type,
            String startAtUtc,
            String endAtUtc,
            String startAtLocal,
            String endAtLocal,
            String timeZone,
            SlotStatus status,
            Integer capacity,
            Long agentId,
            String notes
    ) {}
}

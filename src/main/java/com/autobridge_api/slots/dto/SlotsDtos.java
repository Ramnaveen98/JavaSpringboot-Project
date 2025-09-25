package com.autobridge_api.slots.dto;

import com.autobridge_api.slots.SlotStatus;
import com.autobridge_api.slots.SlotType;

import java.time.Instant;

public class SlotsDtos {

    /** What we return to clients for each calendar slot */
    public record SlotDto(
            Long id,
            SlotType type,
            Instant startAt,
            Instant endAt,
            SlotStatus status,
            Integer capacity,
            Long agentId,
            String notes
    ) {}
}

package com.autobridge_api.requests.dto;

/** Minimal response for command endpoints. */
public record ActionMessageDto(
        Long requestId,
        String status,
        String message
) {}

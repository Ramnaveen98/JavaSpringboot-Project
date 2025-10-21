package com.autobridge_api.requests.dto;

import jakarta.validation.constraints.NotNull;

public final class RequestCommandDtos {

    // already had this
    public record AssignAgentRequest(@NotNull Long agentId,Long userId, String email) {}

    // already had this (optional field)
    public record CancelRequest(String reason) {}

    // NEW: minimal message wrapper used by controller write endpoints
    public record CommandMessage(String message) {}
}

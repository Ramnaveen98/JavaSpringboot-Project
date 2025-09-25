package com.autobridge_api.requests.dto;

import jakarta.validation.constraints.NotNull;

public class RequestCommandDtos {


    public record AssignAgentRequest(
            @NotNull Long agentId
    ) {}
    public record CancelRequest(
            String reason
    ) {}
}

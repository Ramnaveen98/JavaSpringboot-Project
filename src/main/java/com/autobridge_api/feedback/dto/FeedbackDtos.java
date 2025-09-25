package com.autobridge_api.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class FeedbackDtos {

    public record CreateFeedbackRequest(
            @Min(1) @Max(5) int rating,
            @Size(max = 1000) String comment
    ) {}

    public record FeedbackDto(
            Long id,
            Long requestId,
            Integer rating,
            String comment,
            String createdAtUtc
    ) {}
}

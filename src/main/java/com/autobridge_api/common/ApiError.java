package com.autobridge_api.common;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String path,
        int status,
        String error,     // e.g., "Bad Request"
        String message,   // human-friendly
        Instant timestamp,
        List<FieldViolation> fieldErrors
) {
    public static record FieldViolation(String field, String message) {}
}

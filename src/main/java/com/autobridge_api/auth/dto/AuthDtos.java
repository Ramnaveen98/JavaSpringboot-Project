package com.autobridge_api.auth.dto;

import com.autobridge_api.auth.AccountRole;

public class AuthDtos {
    public record SignupRequest(
            String firstName,
            String lastName,
            String email,
            String password,
            String phone,
            AccountRole accountType,
            String inviteCode
    ) {}

    public record LoginRequest(String email, String password) {}

    public record AuthResponse(Long userId, String email, String role, String token) {}
}

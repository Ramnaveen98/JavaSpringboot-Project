package com.autobridge_api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication & Sign-up")
@RestController
@RequestMapping("/api/v1/auth")
public class AgentSignupController {
    private final AgentSignupService service;
    public AgentSignupController(AgentSignupService service) { this.service = service; }

    @Operation(summary = "Public: apply as an agent (inactive until admin approval)")
    @PostMapping("/agent-signup")
    public ResponseEntity<AgentSignupResponse> signup(@RequestBody AgentSignupRequest body) {
        return ResponseEntity.ok(service.signup(body));
    }
}

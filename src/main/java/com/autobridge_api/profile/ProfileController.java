package com.autobridge_api.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "View and edit the current user's profile")
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }

    @Operation(summary = "Get my profile")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDto> getMine() {
        return ResponseEntity.ok(service.getMine());
    }

    @Operation(summary = "Update my profile")
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDto> updateMine(@RequestBody UpdateProfileRequest body) {
        return ResponseEntity.ok(service.updateMine(body));
    }
}

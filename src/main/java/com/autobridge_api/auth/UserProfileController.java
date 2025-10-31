package com.autobridge_api.auth;

import com.autobridge_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserAccountRepository users;
    private final JwtService jwtService; // you already use this elsewhere

    @Autowired
    public UserProfileController(JwtService jwtService, UserAccountRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }


    // Only the fields that exist on UserAccount (no address fields)
    public record ProfileDto(
            String firstName,
            String lastName,
            String email,
            String phone
    ) {}

    private String requireEmailFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return email;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileDto> me(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth
    ) {
        String email = requireEmailFromToken(auth);
        UserAccount user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        ProfileDto dto = new ProfileDto(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone()
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    public ResponseEntity<Void> update(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @RequestBody ProfileDto body
    ) {
        String email = requireEmailFromToken(auth);
        UserAccount user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Update only fields that actually exist in UserAccount
        if (body.firstName() != null) user.setFirstName(body.firstName());
        if (body.lastName()  != null) user.setLastName(body.lastName());
        if (body.email()     != null) user.setEmail(body.email());   // respects unique constraint
        if (body.phone()     != null) user.setPhone(body.phone());

        users.save(user);
        return ResponseEntity.noContent().build();
    }
}

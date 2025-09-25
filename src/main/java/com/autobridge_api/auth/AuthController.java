package com.autobridge_api.auth;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.agents.AgentRepository;
import com.autobridge_api.auth.dto.AuthDtos.*;
import com.autobridge_api.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAccountRepository users;
    private final AgentRepository agents;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    private final String adminInviteCode;
    private final String agentInviteCode;

    public AuthController(UserAccountRepository users,
                          AgentRepository agents,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          JwtService jwt,
                          @Value("${app.adminInviteCode:}") String adminInviteCode,
                          @Value("${app.agentInviteCode:}") String agentInviteCode) {
        this.users = users;
        this.agents = agents;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.adminInviteCode = adminInviteCode;
        this.agentInviteCode = agentInviteCode;
    }


    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<SignupResponse> signup(@Validated @RequestBody SignupRequest body) {
        String email = body.email().toLowerCase();

        if (users.existsByEmail(email)) {
            return ResponseEntity.status(409).build();
        }

        AccountRole role = (body.accountType() == null) ? AccountRole.USER : body.accountType();


        if (role == AccountRole.ADMIN && !safeEquals(body.inviteCode(), adminInviteCode)) {
            return ResponseEntity.status(403).build();
        }
        if (role == AccountRole.AGENT && !safeEquals(body.inviteCode(), agentInviteCode)) {
            return ResponseEntity.status(403).build();
        }

        // Create user
        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(encoder.encode(body.password()))
                .role(role)
                .firstName(body.firstName())
                .lastName(body.lastName())
                .phone(body.phone())
                .active(true)
                .build();
        user = users.save(user);


        if (role == AccountRole.AGENT) {
            Agent a = Agent.builder()
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .active(true)
                    .build();
            agents.save(a);
        }

        // NO TOKEN on signup
        SignupResponse resp = new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                "Sign up successful. Please log in."
        );
        return ResponseEntity.status(201).body(resp);
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest body) {
        var authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.email(), body.password())
        );

        String principalEmail = authentication.getName(); // authenticated email
        UserAccount user = users.findByEmail(principalEmail).orElseThrow();

        String token = jwt.generate(user.getEmail(), user.getRole().name());

        LoginResponse resp = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                token,
                "Login successful"
        );
        return ResponseEntity.ok(resp);
    }


    private static boolean safeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }


    public record SignupResponse(Long userId, String email, String role, String message) {}
    public record LoginResponse(Long userId, String email, String role, String token, String message) {}
}

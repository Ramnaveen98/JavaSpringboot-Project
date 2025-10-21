package com.autobridge_api.auth;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.agents.AgentRepository;
import com.autobridge_api.auth.dto.AuthDtos;
import com.autobridge_api.auth.AccountRole;
import com.autobridge_api.security.JwtService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

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

    // ---------- SIGN UP ----------
    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<AuthDtos.AuthResponse> signup(@Validated @RequestBody AuthDtos.SignupRequest body) {
        final String email = body.email() == null ? null : body.email().trim().toLowerCase();
        if (email == null || body.password() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (users.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
        }

        AccountRole role = (body.accountType() == null) ? AccountRole.USER : body.accountType();

        if (role == AccountRole.ADMIN && !safeEquals(body.inviteCode(), adminInviteCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403
        }
        if (role == AccountRole.AGENT && !safeEquals(body.inviteCode(), agentInviteCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403
        }

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
            agents.save(Agent.builder()
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .active(true)
                    .build());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthDtos.AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), null));
    }

    // ---------- LOGIN ----------
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@RequestBody AuthDtos.LoginRequest body) {
        try {
            final String email = body.email() == null ? "" : body.email().trim().toLowerCase();
            var authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, body.password())
            );

            String principalEmail = authentication.getName();
            UserAccount user = users.findByEmail(principalEmail).orElseThrow();

            if (!isAccountActive(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String token = jwt.generate(user.getEmail(), user.getRole().name());
            return ResponseEntity.ok(
                    new AuthDtos.AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), token)
            );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // ---------- helpers ----------
    private static boolean safeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    /** Works whether the entity exposes `isActive()` or `getActive()`. Defaults to true if neither exists. */
    private static boolean isAccountActive(UserAccount user) {
        try {
            Method m = user.getClass().getMethod("isActive");
            Object v = m.invoke(user);
            if (v instanceof Boolean b) return b;
        } catch (NoSuchMethodException ignored) {
            // fall through
        } catch (Exception e) {
            return true;
        }
        try {
            Method m = user.getClass().getMethod("getActive");
            Object v = m.invoke(user);
            if (v instanceof Boolean b) return b;
        } catch (NoSuchMethodException ignored) {
            // fall through
        } catch (Exception e) {
            return true;
        }
        return true; // if property missing, don't block login
    }
}

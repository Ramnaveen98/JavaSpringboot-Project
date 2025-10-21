package com.autobridge_api.admin;

import com.autobridge_api.auth.UserAccount;
import com.autobridge_api.auth.UserAccountRepository;
import com.autobridge_api.auth.AccountRole; // Make sure this matches your Role enum package
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/agents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAgentsController {

    private final UserAccountRepository users;

    public AdminAgentsController(UserAccountRepository users) {
        this.users = users;
    }

    // ----- DTOs -----

    public record AgentDto(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone
    ) {
        static AgentDto from(UserAccount ua) {
            return new AgentDto(
                    ua.getId(),
                    nullSafe(ua.getFirstName()),
                    nullSafe(ua.getLastName()),
                    nullSafe(ua.getEmail()),
                    nullSafe(ua.getPhone())
            );
        }
    }

    public static class UpsertAgentReq {
        @NotBlank(message = "First name is required")
        public String firstName;

        @NotBlank(message = "Last name is required")
        public String lastName;

        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        public String email;

        public String phone;
        public String password; // optional; set only on create
    }

    private static String nullSafe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    // ----- Endpoints -----

    @GetMapping
    public List<AgentDto> listAgents() {
        // Filter in memory to avoid depending on repository methods you may not have.
        return users.findAll().stream()
                .filter(ua -> ua.getRole() == AccountRole.AGENT) // adjust if your Role enum differs
                .map(AgentDto::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentDto createAgent(@Valid @RequestBody UpsertAgentReq req) {
        // Basic uniqueness check
        if (users.findAll().stream().anyMatch(u -> req.email.equalsIgnoreCase(Objects.toString(u.getEmail(), "")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        UserAccount ua = new UserAccount();
        ua.setFirstName(req.firstName);
        ua.setLastName(req.lastName);
        ua.setEmail(req.email);
        ua.setPhone(req.phone);
        ua.setRole(AccountRole.AGENT);
        // If your entity hashes passwords itself or you have a service for it, wire that up here:
        // TODO: set password if needed. For now, leave null to trigger your signup/invite flow.
        return AgentDto.from(users.save(ua));
    }

    @PutMapping("/{id}")
    public AgentDto updateAgent(@PathVariable Long id, @Valid @RequestBody UpsertAgentReq req) {
        UserAccount ua = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        if (ua.getRole() != AccountRole.AGENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an agent");
        }

        // prevent email collisions
        boolean emailTaken = users.findAll().stream()
                .anyMatch(u -> !Objects.equals(u.getId(), id)
                        && req.email.equalsIgnoreCase(Objects.toString(u.getEmail(), "")));
        if (emailTaken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        ua.setFirstName(req.firstName);
        ua.setLastName(req.lastName);
        ua.setEmail(req.email);
        ua.setPhone(req.phone);
        // TODO: if you want to allow admin to reset password, do it here.

        return AgentDto.from(users.save(ua));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable Long id) {
        UserAccount ua = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
        if (ua.getRole() != AccountRole.AGENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an agent");
        }
        users.deleteById(id);
    }
}

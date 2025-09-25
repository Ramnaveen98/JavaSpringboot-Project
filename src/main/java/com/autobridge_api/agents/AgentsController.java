package com.autobridge_api.agents;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Agents", description = "Manage agents")
@RestController
@RequestMapping("/api/v1/agents")
public class AgentsController {

    private final AgentRepository repo;

    public AgentsController(AgentRepository repo) {
        this.repo = repo;
    }

    public record CreateAgentRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            String phone,
            Boolean active
    ){}

    public record ToggleActiveRequest(@NotNull Boolean active){}

    public record AgentDto(
            Long id, String firstName, String lastName, String email, String phone, boolean active
    ){}

    private static AgentDto toDto(Agent a) {
        return new AgentDto(
                a.getId(), a.getFirstName(), a.getLastName(), a.getEmail(), a.getPhone(), a.isActive()
        );
    }

    @Operation(summary = "Create an agent")
    @PostMapping
    public ResponseEntity<AgentDto> create(@Valid @RequestBody CreateAgentRequest body) {
        Agent a = new Agent();
        a.setFirstName(body.firstName());
        a.setLastName(body.lastName());
        a.setEmail(body.email());
        a.setPhone(body.phone());
        a.setActive(body.active() == null ? true : body.active());
        a = repo.save(a);
        return ResponseEntity.created(URI.create("/api/v1/agents/" + a.getId())).body(toDto(a));
    }

    @Operation(summary = "List agents (optionally only active)")
    @GetMapping
    public List<AgentDto> list(@RequestParam(required = false) Boolean active) {
        List<Agent> list = (active == null)
                ? repo.findAll()
                : repo.findByActive(active);
        return list.stream().map(AgentsController::toDto).toList();
    }

    @Operation(summary = "Activate / deactivate an agent")
    @PatchMapping("/{id}/active")
    public ResponseEntity<AgentDto> setActive(@PathVariable Long id, @Valid @RequestBody ToggleActiveRequest body) {
        Agent a = repo.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Agent not found"));
        a.setActive(body.active());
        a = repo.save(a);
        return ResponseEntity.ok(toDto(a));
    }
}

package com.autobridge_api.admin;

import com.autobridge_api.requests.ServiceRequest;
import com.autobridge_api.requests.ServiceRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Requests API
 *
 * This controller intentionally supports TWO base paths so older UI code keeps working:
 *   - /api/v1/admin/requests
 *   - /api/v1/requests/admin
 *
 * All method-level mappings are relative so they work for both bases.
 *
 * NOTES
 *  - The assign endpoint delegates to ServiceRequestService.assign(id, agentIdOrUserId)
 *    which accepts either an agents.id OR a users.id (resolved via email).
 */
@RestController
@RequestMapping({"/api/v1/admin/requests", "/api/v1/requests/admin"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminRequestsController {

    /** Row DTO your frontend already expects. */
    public record RequestRowDto(
            Long id,
            String serviceName,
            String status,
            String assignedAgentName
    ) {}

    /** Command DTOs */
    public record AssignDto(Long agentId) {}
    public record CancelDto(String reason) {}
    public record Message(String message) {}

    private final RequestAdminService adminSvc;   // <-- top-level interface (see next file)
    private final ServiceRequestService requestSvc;

    public AdminRequestsController(RequestAdminService adminSvc,
                                   ServiceRequestService requestSvc) {
        this.adminSvc = adminSvc;
        this.requestSvc = requestSvc;
    }

    // Works for BOTH base paths
    @GetMapping({"", "/"})
    public ResponseEntity<List<RequestRowDto>> list() {
        return ResponseEntity.ok(adminSvc.listAdminRows());
    }

    /**
     * Assign / reassign.
     * Body: { "agentId": <number> }
     * - agentId may be an agents.id OR a users.id (resolved to agent by email).
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<Message> assign(@PathVariable long id,
                                          @Validated @RequestBody AssignDto body) {
        if (body == null || body.agentId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Message("agentId is required"));
        }

        ServiceRequest updated = requestSvc.assign(id, body.agentId());

        String agentName = (updated.getAssignedAgent() == null)
                ? "Agent"
                : ((safe(updated.getAssignedAgent().getFirstName()) + " " +
                safe(updated.getAssignedAgent().getLastName())).trim());
        if (agentName.isBlank()) agentName = "Agent";

        return ResponseEntity.ok(new Message(agentName + " assigned."));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Message> complete(@PathVariable long id) {
        requestSvc.complete(id);
        return ResponseEntity.ok(new Message("Request completed."));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Message> cancel(@PathVariable long id,
                                          @RequestBody(required = false) CancelDto body) {
        adminSvc.cancelRequest(id, body == null ? null : body.reason());
        return ResponseEntity.ok(new Message("Request cancelled."));
    }

    private static String safe(String v) { return v == null ? "" : v; }
}

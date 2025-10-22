// src/main/java/com/autobridge_api/agent/AgentRequestsController.java
package com.autobridge_api.agent;

import com.autobridge_api.requests.ServiceRequest;
import com.autobridge_api.requests.ServiceRequestRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agent/requests")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public class AgentRequestsController {

    private final ServiceRequestRepository requests;

    public AgentRequestsController(ServiceRequestRepository requests) {
        this.requests = requests;
    }

    /**
     * List requests assigned to the current agent (by their email).
     * Frontend calls: GET /api/v1/agent/requests/mine
     */
    @GetMapping("/mine")
    public ResponseEntity<List<RowDto>> mine(Authentication auth,
                                             @RequestParam(defaultValue = "America/Detroit") String tz) {
        final String email = auth.getName();
        final String emailLc = email == null ? "" : email.toLowerCase();

        // query only my assignments, newest first
        List<ServiceRequest> items = requests.findAll(
                (root, q, cb) -> cb.and(
                        cb.isNotNull(root.get("assignedAgent")),
                        cb.equal(cb.lower(root.get("assignedAgent").get("email")), emailLc)
                ),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        final ZoneId zone = ZoneId.of(tz);
        final DateTimeFormatter localFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);

        List<RowDto> rows = items.stream().map(r -> {
            String serviceName = (r.getService() != null ? r.getService().getName() : "Unknown");
            String slotStartAtLocal = (r.getSlot() != null && r.getSlot().getStartAt() != null)
                    ? localFmt.format(r.getSlot().getStartAt())
                    : null;
            Long vehicleId = (r.getInventoryVehicle() != null ? r.getInventoryVehicle().getId() : null);
            String agentEmail = (r.getAssignedAgent() != null ? r.getAssignedAgent().getEmail() : null);

            return new RowDto(
                    r.getId(),
                    serviceName,
                    r.getStatus() != null ? r.getStatus().name() : "PENDING",
                    slotStartAtLocal,
                    vehicleId,
                    agentEmail
            );
        }).toList();

        return ResponseEntity.ok(rows);
    }

    // (Optional) keep the old bare GET for compatibility if something in the UI still calls it.
    @GetMapping
    public ResponseEntity<List<RowDto>> myAssignments(Authentication auth,
                                                      @RequestParam(defaultValue = "America/Detroit") String tz) {
        return mine(auth, tz);
    }

    // Minimal DTO the Agent dashboard uses
    public record RowDto(
            Long id,
            String serviceName,
            String status,
            String slotStartAtLocal,
            Long inventoryVehicleId,
            String agentEmail
    ) {}
}


// src/main/java/com/autobridge_api/requests/RequestsMineController.java
package com.autobridge_api.requests;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestsMineController {

    private final ServiceRequestRepository repo;

    public RequestsMineController(ServiceRequestRepository repo) {
        this.repo = repo;
    }

    public record MineRowDto(
            Long id,
            String serviceName,
            String status,
            String slotStartAtLocal,   // ISO string for frontend
            Long inventoryVehicleId,
            String agentEmail
    ) {}

    @GetMapping("/mine")
    public ResponseEntity<List<MineRowDto>> mine(Authentication auth) {
        final String email = auth.getName();
        var rows = repo.findMineRows(email).stream().map(p -> new MineRowDto(
                p.getId(),
                p.getServiceName(),
                p.getStatus().name(),
                p.getSlotStartAtLocal() == null ? null : p.getSlotStartAtLocal().toString(),
                p.getInventoryVehicleId(),
                p.getAgentEmail()
        )).toList();
        return ResponseEntity.ok(rows);
    }
}

/*package com.autobridge_api.requests;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

//@RestController
//@RequestMapping("/api/v1/requests")
public class RequestsMineController {

    private final ServiceRequestRepository requests;

    public RequestsMineController(ServiceRequestRepository requests) {
        this.requests = requests;
    }

    public record RequestDto(
            Long id,
            String service,
            String status,
            LocalDateTime createdAt
    ) {}

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RequestDto>> mine(Authentication auth) {
        final String email = auth.getName();

        List<ServiceRequestRepository.MineRow> rows = requests.findMineRows(email);

        List<RequestDto> out = rows.stream().map(r ->
                new RequestDto(
                        r.getId(),
                        r.getServiceName(),
                        r.getStatus() == null ? "PENDING" : r.getStatus().name(),
                        r.getCreatedAt()
                )
        ).toList();

        return ResponseEntity.ok(out);
    }
}

 */


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

package com.autobridge_api.requests;

import com.autobridge_api.requests.dto.PageResponse;
import com.autobridge_api.requests.dto.RequestCommandDtos;
import com.autobridge_api.requests.dto.RequestCommandDtos.AssignAgentRequest;
import com.autobridge_api.requests.dto.RequestCommandDtos.CancelRequest;
import com.autobridge_api.requests.dto.RequestDtos;
import com.autobridge_api.requests.dto.RequestDtos.ServiceRequestDto;
import com.autobridge_api.slots.Slot;
import com.autobridge_api.vehicles.InventoryVehicle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "Requests", description = "Create and view service requests")
@RestController
@RequestMapping("/api/v1/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }


    @Operation(summary = "Create a request (books the slot)")
    @PostMapping
    public ResponseEntity<ServiceRequestDto> create(@Validated @RequestBody RequestDtos.CreateServiceRequestRequest body,
                                                    @RequestParam(defaultValue = "America/Detroit") String tz) {
        ServiceRequest saved = service.create(body);
        return ResponseEntity.status(201).body(toDto(saved, tz));
    }

    @Operation(summary = "Get a request by id")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ServiceRequestDto> get(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "America/Detroit") String tz) {
        return ResponseEntity.ok(toDto(service.getById(id), tz));
    }


    @Operation(summary = "List requests: all or filtered by status, with pagination")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<ServiceRequestDto>> list(
            @RequestParam(required = false) RequestStatus status,   // PENDING/ASSIGNED/IN_PROGRESS/COMPLETED/CANCELLED
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "America/Detroit") String tz
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ServiceRequest> result = (status == null)
                ? service.listAll(pageable)
                : service.listByStatus(status, pageable);

        var dtos = result.getContent().stream().map(r -> toDto(r, tz)).toList();

        PageResponse<ServiceRequestDto> body = new PageResponse<>(
                dtos,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );

        return ResponseEntity.ok(body);
    }



    @Operation(summary = "ADMIN: Search by status/agent/date with pagination")
    @GetMapping("/admin")
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<ServiceRequestDto>> adminList(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "America/Detroit") String tz
    ) {
        ZoneId zone = ZoneId.of(tz);
        Instant from = (fromDate != null) ? fromDate.atStartOfDay(zone).toInstant() : null;
        Instant to   = (toDate   != null) ? toDate.plusDays(1).atStartOfDay(zone).toInstant() : null;

        Specification<ServiceRequest> spec = RequestSpecs.byFilters(status, agentId, from, to);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ServiceRequest> result = service.search(spec, pageable);
        var dtos = result.getContent().stream().map(r -> toDto(r, tz)).toList();

        PageResponse<ServiceRequestDto> body = new PageResponse<>(
                dtos, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast()
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "ADMIN: Export requests as CSV (same filters as /admin)")
    @GetMapping(value = "/admin/export", produces = "text/csv")
    @Transactional(readOnly = true)
    public void exportCsv(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "America/Detroit") String tz,
            HttpServletResponse response
    ) throws IOException {
        ZoneId zone = ZoneId.of(tz);
        Instant from = (fromDate != null) ? fromDate.atStartOfDay(zone).toInstant() : null;
        Instant to   = (toDate   != null) ? toDate.plusDays(1).atStartOfDay(zone).toInstant() : null;

        Specification<ServiceRequest> spec = RequestSpecs.byFilters(status, agentId, from, to);
        List<ServiceRequest> items = service.searchAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        response.setHeader("Content-Disposition", "attachment; filename=\"requests.csv\"");
        var w = response.getWriter();

        // header
        w.println("id,status,createdAtLocal,service,slotId,agentId,agentName,userName,userEmail,city,state");

        DateTimeFormatter localFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);

        for (ServiceRequest r : items) {
            var agent = r.getAssignedAgent();
            String agentIdStr = (agent != null && agent.getId() != null) ? agent.getId().toString() : "";
            String agentName  = (agent != null) ? (nullSafe(agent.getFirstName()) + " " + nullSafe(agent.getLastName())).trim() : "";

            String createdLocal = (r.getCreatedAt() != null) ? localFmt.format(r.getCreatedAt()) : "";

            String userName = (nullSafe(r.getUserFirstName()) + " " + nullSafe(r.getUserLastName())).trim();

            w.append(csv(r.getId()))
                    .append(',').append(csv(r.getStatus().name()))
                    .append(',').append(csv(createdLocal))
                    .append(',').append(csv(r.getService().getName()))
                    .append(',').append(csv(r.getSlot().getId()))
                    .append(',').append(csv(agentIdStr))
                    .append(',').append(csv(agentName))
                    .append(',').append(csv(userName))
                    .append(',').append(csv(r.getUserEmail()))
                    .append(',').append(csv(r.getCity()))
                    .append(',').append(csv(r.getState()))
                    .append('\n');
        }
        w.flush();
    }

    private static String nullSafe(String s) { return (s == null) ? "" : s; }
    private static String csv(Object v) {
        String s = (v == null) ? "" : String.valueOf(v);
        s = s.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }


    @Operation(summary = "Assign request to an agent (PENDING → ASSIGNED)")
    @PostMapping("/{id}/assign")
    public ResponseEntity<ServiceRequestDto> assign(@PathVariable Long id,
                                                    @Validated @RequestBody AssignAgentRequest body,
                                                    @RequestParam(defaultValue = "America/Detroit") String tz) {
        ServiceRequest updated = service.assign(id, body.agentId());
        return ResponseEntity.status(HttpStatus.OK).body(toDto(updated, tz));
    }

    @Operation(summary = "Mark request started (ASSIGNED → IN_PROGRESS)")
    @PostMapping("/{id}/start")
    public ResponseEntity<ServiceRequestDto> start(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "America/Detroit") String tz) {
        ServiceRequest updated = service.start(id);
        return ResponseEntity.ok(toDto(updated, tz));
    }

    @Operation(summary = "Complete request (IN_PROGRESS → COMPLETED; slot → CONSUMED)")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ServiceRequestDto> complete(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "America/Detroit") String tz) {
        ServiceRequest updated = service.complete(id);
        return ResponseEntity.ok(toDto(updated, tz));
    }

    @Operation(summary = "Cancel request (PENDING/ASSIGNED/IN_PROGRESS → CANCELLED; slot re-opens)")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ServiceRequestDto> cancel(@PathVariable Long id,
                                                    @RequestBody(required = false) RequestCommandDtos.CancelRequest body,
                                                    @RequestParam(defaultValue = "America/Detroit") String tz) {
        ServiceRequest updated = service.cancel(id);
        return ResponseEntity.ok(toDto(updated, tz));
    }


    private ServiceRequestDto toDto(ServiceRequest r, String tz) {
        ZoneId zone = ZoneId.of(tz);
        DateTimeFormatter localFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);
        DateTimeFormatter utcFmt   = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

        Slot s = r.getSlot();
        InventoryVehicle v = r.getInventoryVehicle();

        Long agentId = null;
        String agentName = null;
        if (r.getAssignedAgent() != null) {
            agentId = r.getAssignedAgent().getId();
            String fn = r.getAssignedAgent().getFirstName();
            String ln = r.getAssignedAgent().getLastName();
            agentName = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
        }

        return new ServiceRequestDto(
                r.getId(),
                r.getStatus().name(),
                r.getCreatedAt() != null ? utcFmt.format(r.getCreatedAt()) : null,
                r.getUpdatedAt() != null ? utcFmt.format(r.getUpdatedAt()) : null,

                r.getService().getId(),
                r.getService().getSlug(),
                r.getService().getName(),

                s.getId(),
                utcFmt.format(s.getStartAt()),
                utcFmt.format(s.getEndAt()),
                localFmt.format(s.getStartAt()),
                localFmt.format(s.getEndAt()),
                zone.getId(),

                (v != null ? v.getId() : null),
                (v != null ? v.getMake().getName() : null),
                (v != null ? v.getModel().getName() : null),
                (v != null ? v.getYear() : null),
                (v != null ? v.getVin() : null),

                r.getUserFirstName(),
                r.getUserLastName(),
                r.getUserEmail(),
                r.getUserPhone(),

                r.getAddressLine1(),
                r.getAddressLine2(),
                r.getCity(),
                r.getState(),
                r.getPostalCode(),
                r.getCountry(),

                r.getNotes(),

                // NEW (optional fields you added in Phase 4)
                agentId,
                agentName
        );
    }
}

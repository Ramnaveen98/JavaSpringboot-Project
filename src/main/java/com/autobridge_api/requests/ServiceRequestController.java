/*package com.autobridge_api.requests;

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
*/



// src/main/java/com/autobridge_api/requests/ServiceRequestController.java
package com.autobridge_api.requests;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.auth.UserAccount;
import com.autobridge_api.auth.UserAccountRepository;
import com.autobridge_api.requests.dto.PageResponse;
import com.autobridge_api.requests.dto.RequestCommandDtos.AssignAgentRequest;
import com.autobridge_api.requests.dto.RequestCommandDtos.CancelRequest;
import com.autobridge_api.requests.dto.RequestCommandDtos.CommandMessage;
import com.autobridge_api.requests.dto.RequestDtos;
import com.autobridge_api.requests.dto.RequestDtos.ServiceRequestDto;
import com.autobridge_api.security.JwtService;
import com.autobridge_api.servicecatalog.ServiceOffering;
import com.autobridge_api.servicecatalog.ServiceOfferingRepository;
import com.autobridge_api.slots.Slot;
import com.autobridge_api.slots.SlotRepository;
import com.autobridge_api.slots.SlotStatus;
import com.autobridge_api.vehicles.InventoryVehicle;
import com.autobridge_api.vehicles.InventoryVehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Tag(name = "Requests", description = "Create and view service requests")
@RestController
@RequestMapping("/api/v1/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;
    private final ServiceRequestRepository requestRepo;
    private final ServiceOfferingRepository serviceRepo;
    private final InventoryVehicleRepository inventoryRepo;
    private final SlotRepository slotRepo;
    private final UserAccountRepository users;
    private final JwtService jwtService;
    private final ObjectMapper om;

    public ServiceRequestController(ServiceRequestService service,
                                    ServiceRequestRepository requestRepo,
                                    ServiceOfferingRepository serviceRepo,
                                    InventoryVehicleRepository inventoryRepo,
                                    SlotRepository slotRepo,
                                    UserAccountRepository users,
                                    JwtService jwtService,
                                    ObjectMapper om) {
        this.service = service;
        this.requestRepo = requestRepo;
        this.serviceRepo = serviceRepo;
        this.inventoryRepo = inventoryRepo;
        this.slotRepo = slotRepo;
        this.users = users;
        this.jwtService = jwtService;
        this.om = om;
    }

    // ---------- CREATE ----------
    @Operation(summary = "Create a request (books the slot)")
    @PostMapping
    @Transactional
    public ResponseEntity<CommandMessage> create(
            @Validated @RequestBody RequestDtos.CreateServiceRequestRequest body,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @RequestParam(defaultValue = "America/Detroit") String tz) {

        if (body.serviceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceId is required");
        }

        // If client already provides a slotId, use the standard service path
        if (body.slotId() != null) {
            service.create(body);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CommandMessage("Your Slot is booked! Thank you for the booking. Searching for the agent to assign"));
        }

        // 1) Try to get a local datetime via common accessors on the DTO (record/getter styles).
        Object dtValue = firstNonNullViaReflection(
                body,
                // record-style
                "scheduledAt", "scheduledAtLocal", "requestedAt", "requestedStartAt", "startAtLocal", "startAt",
                // getter-style
                "getScheduledAt", "getScheduledAtLocal", "getRequestedAt", "getRequestedStartAt", "getStartAtLocal", "getStartAt"
        );

        // 2) If still null, inspect the raw JSON map with Jackson and accept many common keys
        if (dtValue == null) {
            Map<String, Object> raw = om.convertValue(body, Map.class);
            String key = firstPresentKey(raw,
                    "scheduledAt", "scheduled_at",
                    "startAt", "start_at",
                    "preferredDateTime", "preferred_date_time",
                    "preferredTime", "preferred_time",
                    "dateTime", "datetime", "time");
            if (key != null) {
                dtValue = raw.get(key);
            }
        }

        if (dtValue == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Either slotId must be provided, or a local datetime field (e.g., scheduledAt) must be set"
            );
        }

        // Auth → current user
        final String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }
        final String email = jwtService.extractEmail(token);
        final UserAccount user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));

        // Domain lookups
        final ServiceOffering svc = serviceRepo.findById(body.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown serviceId"));

        final InventoryVehicle vehicle = (body.inventoryVehicleId() == null) ? null :
                inventoryRepo.findById(body.inventoryVehicleId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown inventoryVehicleId"));

        // Parse datetime. We treat Strings without offset/Z as LOCAL in the provided tz.
        final ZoneId zone = ZoneId.of(tz);
        final Instant startAtUtc = toInstantAssumingLocal(dtValue, zone);
        final Instant endAtUtc = startAtUtc.plus(Duration.ofMinutes(svc.getDurationMinutes()));

        // Create Slot
        Slot slot = new Slot();
        slot.setStartAt(startAtUtc);
        slot.setEndAt(endAtUtc);
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepo.save(slot);

        // Create Request
        ServiceRequest r = new ServiceRequest();
        r.setService(svc);
        r.setSlot(slot);
        r.setInventoryVehicle(vehicle);
        r.setStatus(RequestStatus.PENDING);

        r.setUserFirstName(notBlankOr(body.userFirstName(), user.getFirstName()));
        r.setUserLastName(notBlankOr(body.userLastName(), user.getLastName()));
        r.setUserEmail(notBlankOr(body.userEmail(), user.getEmail()));
        r.setUserPhone(notBlankOr(body.userPhone(), user.getPhone()));

        r.setAddressLine1(emptyToNull(body.addressLine1()));
        r.setAddressLine2(emptyToNull(body.addressLine2()));
        r.setCity(emptyToNull(body.city()));
        r.setState(emptyToNull(body.state()));
        r.setPostalCode(emptyToNull(body.postalCode()));
        r.setCountry(emptyToNull(body.country()));
        r.setNotes(emptyToNull(body.notes()));

        requestRepo.save(r);

        slot.setStatus(SlotStatus.BOOKED);
        slotRepo.save(slot);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommandMessage("Your Slot is booked! Thank you for the booking. Searching for the agent to assign"));
    }

    // ---------- READ ONE ----------
    @Operation(summary = "Get a request by id")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ServiceRequestDto> get(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "America/Detroit") String tz) {
        return ResponseEntity.ok(toDto(service.getById(id), tz));
    }

    // ---------- ADMIN: LIST "MINE BY EMAIL" ----------
    // Absolute path to avoid conflicting with RequestsMineController's /api/v1/requests/mine
    @Operation(summary = "ADMIN: List a user's requests by email")
    @GetMapping("/api/v1/admin/requests/mine")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ServiceRequestDto>> mineByEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "America/Detroit") String tz
    ) {
        // Find all requests for this user, most recent first
        List<ServiceRequest> items = service.searchAll(
                (root, q, cb) -> cb.equal(cb.lower(root.get("userEmail")), email.toLowerCase()),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<ServiceRequestDto> dtos = items.stream().map(r -> toDto(r, tz)).toList();
        return ResponseEntity.ok(dtos);
    }

    // ---------- LIST (paged) ----------
    @Operation(summary = "List requests: all or filtered by status, with pagination")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<ServiceRequestDto>> list(
            @RequestParam(required = false) RequestStatus status,
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

    // ---------- LIFECYCLE COMMANDS ----------
    @Operation(summary = "Assign request to an agent (PENDING → ASSIGNED). Body.agentId can be agents.id OR users.id")
    @PostMapping("/{id}/assign")
    public ResponseEntity<CommandMessage> assign(@PathVariable Long id,
                                                 @Validated @RequestBody AssignAgentRequest body) {
        // Service accepts (requestId, agentIdOrUserId) and resolves agent by id or by related user email
        ServiceRequest updated = service.assign(id, body.agentId());
        Agent ag = updated.getAssignedAgent();
        String agentName = (ag == null) ? "the agent" : (safe(ag.getFirstName()) + " " + safe(ag.getLastName())).trim();
        return ResponseEntity.ok(new CommandMessage((agentName.isBlank() ? "The agent" : agentName) + " has been assigned to this request."));
    }

    @Operation(summary = "Mark request started (ASSIGNED → IN_PROGRESS)")
    @PostMapping("/{id}/start")
    public ResponseEntity<CommandMessage> start(@PathVariable Long id) {
        service.start(id);
        return ResponseEntity.ok(new CommandMessage("Work started. Status is now IN_PROGRESS."));
    }

    @Operation(summary = "Complete request (IN_PROGRESS → COMPLETED; slot → CONSUMED)")
    @PostMapping("/{id}/complete")
    public ResponseEntity<CommandMessage> complete(@PathVariable Long id) {
        service.complete(id);
        return ResponseEntity.ok(new CommandMessage("Request completed. Slot marked as CONSUMED."));
    }

    @Operation(summary = "Cancel request (PENDING/ASSIGNED/IN_PROGRESS → CANCELLED; slot re-opens)")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CommandMessage> cancel(@PathVariable Long id,
                                                 @RequestBody(required = false) CancelRequest body) {
        service.cancel(id);
        return ResponseEntity.ok(new CommandMessage("Request cancelled. Slot is available again."));
    }

    // ---------- Mapper ----------
    private RequestDtos.ServiceRequestDto toDto(ServiceRequest r, String tz) {
        ZoneId zone = ZoneId.of(tz);
        DateTimeFormatter localFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);
        DateTimeFormatter utcFmt   = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

        Slot s = r.getSlot();

        // Be defensive: inventoryVehicle might be a broken proxy or already removed
        InventoryVehicle v = null;
        try {
            v = r.getInventoryVehicle();
            if (v != null) {
                // touch an attribute to force initialization; if the row is gone, this may throw
                v.getId();
            }
        } catch (Exception ignore) {
            v = null;
        }

        var ag = r.getAssignedAgent();
        Long assignedAgentId = (ag != null ? ag.getId() : null);
        String assignedAgentName = (ag != null ? (safe(ag.getFirstName()) + " " + safe(ag.getLastName())).trim() : null);

        return new ServiceRequestDto(
                r.getId(),
                r.getStatus().name(),
                r.getCreatedAt() != null ? utcFmt.format(r.getCreatedAt()) : null,
                r.getUpdatedAt() != null ? utcFmt.format(r.getUpdatedAt()) : null,

                r.getService().getId(),
                r.getService().getSlug(),
                r.getService().getName(),

                (s != null ? s.getId() : null),
                (s != null ? utcFmt.format(s.getStartAt()) : null),
                (s != null ? utcFmt.format(s.getEndAt()) : null),
                (s != null ? localFmt.format(s.getStartAt()) : null),
                (s != null ? localFmt.format(s.getEndAt()) : null),
                zone.getId(),

                // Vehicle mapping (null-safe)
                (v != null ? v.getId() : null),
                (v != null ? v.getBrand() : null),
                (v != null ? v.getModel() : null),
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

                assignedAgentId,
                assignedAgentName
        );
    }

    // ---------- helpers ----------

    private static String notBlankOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private static Object firstNonNullViaReflection(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                Object val = m.invoke(target);
                if (val != null) return val;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid value for " + name + ": " + ex.getMessage());
            }
        }
        return null;
    }

    /** Case-insensitive key search over a converted JSON map. */
    private static String firstPresentKey(Map<String, Object> map, String... candidates) {
        if (map == null) return null;
        for (String c : candidates) {
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(c) && e.getValue() != null) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Accepts String / LocalDateTime / OffsetDateTime / Instant.
     * Strings without offset/Z are treated as LOCAL in the given zone (e.g. "2025-10-13T10:30" or "...:30:00").
     */
    private static Instant toInstantAssumingLocal(Object value, ZoneId zone) {
        if (value instanceof Instant i) return i;
        if (value instanceof OffsetDateTime odt) return odt.toInstant();
        if (value instanceof LocalDateTime ldt) return ldt.atZone(zone).toInstant();
        if (value instanceof String s) {
            String trimmed = s.trim();
            // pad seconds if missing: yyyy-MM-ddTHH:mm -> yyyy-MM-ddTHH:mm:00
            if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) trimmed = trimmed + ":00";

            // If explicit offset/Z → parse as instant; else treat as local in zone.
            if (trimmed.endsWith("Z") || trimmed.matches(".*[+\\-]\\d{2}:\\d{2}$")) {
                try {
                    return OffsetDateTime.parse(trimmed).toInstant();
                } catch (DateTimeParseException ex) {
                    return Instant.parse(trimmed); // try plain Instant
                }
            }
            try {
                LocalDateTime ldt = LocalDateTime.parse(trimmed);
                return ldt.atZone(zone).toInstant();
            } catch (DateTimeParseException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "scheduledAt must be an ISO datetime like 2025-10-13T10:30 or 2025-10-13T10:30:00"
                );
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unsupported datetime type: " + value.getClass().getSimpleName());
    }
}

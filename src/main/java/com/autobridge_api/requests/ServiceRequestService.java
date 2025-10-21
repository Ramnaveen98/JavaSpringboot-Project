package com.autobridge_api.requests;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.agents.AgentRepository;
import com.autobridge_api.auth.AccountRole;
import com.autobridge_api.auth.UserAccount;
import com.autobridge_api.auth.UserAccountRepository;
import com.autobridge_api.requests.dto.RequestDtos.CreateServiceRequestRequest;
import com.autobridge_api.servicecatalog.ServiceOffering;
import com.autobridge_api.servicecatalog.ServiceOfferingRepository;
import com.autobridge_api.slots.Slot;
import com.autobridge_api.slots.SlotRepository;
import com.autobridge_api.slots.SlotStatus;
import com.autobridge_api.slots.SlotType;
import com.autobridge_api.vehicles.InventoryVehicle;
import com.autobridge_api.vehicles.InventoryVehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepo;
    private final ServiceOfferingRepository offeringRepo;
    private final InventoryVehicleRepository vehicleRepo;
    private final SlotRepository slotRepo;
    private final AgentRepository agentRepo;
    private final UserAccountRepository userRepo;

    public ServiceRequestService(ServiceRequestRepository requestRepo,
                                 ServiceOfferingRepository offeringRepo,
                                 InventoryVehicleRepository vehicleRepo,
                                 SlotRepository slotRepo,
                                 AgentRepository agentRepo,
                                 UserAccountRepository userRepo) {
        this.requestRepo = requestRepo;
        this.offeringRepo = offeringRepo;
        this.vehicleRepo = vehicleRepo;
        this.slotRepo = slotRepo;
        this.agentRepo = agentRepo;
        this.userRepo = userRepo;
    }

    /**
     * Create a request:
     *  - If slotId is given, book that AVAILABLE slot.
     *  - Else, if scheduledAt is given (local datetime), create a Slot for that time and book it.
     */
    @Transactional
    public ServiceRequest create(CreateServiceRequestRequest body) {
        if (body == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required");
        if (body.serviceId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceId is required");

        // Service offering
        ServiceOffering offering = offeringRepo.findById(body.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service offering not found"));

        // Optional vehicle
        InventoryVehicle vehicle = null;
        if (body.inventoryVehicleId() != null) {
            vehicle = vehicleRepo.findById(body.inventoryVehicleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory vehicle not found"));
        }

        // Log exactly what we received for easier debugging
        log.info("Create request: svc={}, invVeh={}, slotId={}, scheduledAt={}, lastName={}, email={}, addr1={}, city={}",
                body.serviceId(),
                body.inventoryVehicleId(),
                body.slotId(),
                tryGetScheduledAt(body),
                body.userLastName(),
                body.userEmail(),
                body.addressLine1(),
                body.city()
        );

        // Resolve Slot
        Slot slot;
        if (body.slotId() != null) {
            slot = slotRepo.findById(body.slotId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is not available");
            }
            // Mark booked
            slot.setStatus(SlotStatus.BOOKED);
            slot = slotRepo.save(slot);
        } else {
            // Try to read scheduledAt from the record (record may or may not declare it)
            String scheduledAt = tryGetScheduledAt(body);
            if (scheduledAt == null || scheduledAt.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Either slotId must be provided, or scheduledAt must be set"
                );
            }

            LocalDateTime local = parseLocalDateTime(scheduledAt);
            ZoneId zone = ZoneId.of("America/Detroit");
            ZonedDateTime start = local.atZone(zone);

            int minutes = (offering.getDurationMinutes() != null && offering.getDurationMinutes() > 0)
                    ? offering.getDurationMinutes()
                    : 60;
            ZonedDateTime end = start.plusMinutes(minutes);

            slot = new Slot();
            slot.setStartAt(start.toInstant());
            slot.setEndAt(end.toInstant());
            slot.setCapacity(1);
            slot.setNotes("Auto-created from scheduledAt");
            slot.setStatus(SlotStatus.BOOKED);

            // CRITICAL: force non-null type for DB insert
            slot.setType(SlotType.SERVICE);

            slot = slotRepo.save(slot);
        }

        // Build request entity
        ServiceRequest req = ServiceRequest.builder()
                .service(offering)
                .inventoryVehicle(vehicle)
                .slot(slot)
                .status(RequestStatus.PENDING)
                .userFirstName(body.userFirstName())
                .userLastName(body.userLastName())
                .userEmail(body.userEmail())
                .userPhone(body.userPhone())
                .addressLine1(body.addressLine1())
                .addressLine2(body.addressLine2())
                .city(body.city())
                .state(body.state())
                .postalCode(body.postalCode())
                .country(body.country())
                .notes(body.notes())
                .build();

        // Extra belt & suspenders before persist (in addition to @PrePersist)
        if (req.getUserLastName() == null || req.getUserLastName().isBlank()) req.setUserLastName("N/A");
        if (req.getUserEmail() == null || req.getUserEmail().isBlank()) req.setUserEmail("no-email@local");
        if (req.getAddressLine1() == null || req.getAddressLine1().isBlank()) req.setAddressLine1("N/A");
        if (req.getCity() == null || req.getCity().isBlank()) req.setCity("N/A");

        return requestRepo.save(req);
    }

    // ---------- Reads / searches ----------

    @Transactional(readOnly = true)
    public ServiceRequest getById(Long id) {
        return requestRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequest> listAll(Pageable pageable) {
        return requestRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequest> listByStatus(RequestStatus status, Pageable pageable) {
        return requestRepo.findAll((root, q, cb) -> cb.equal(root.get("status"), status), pageable);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequest> search(Specification<ServiceRequest> spec, Pageable pageable) {
        return requestRepo.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequest> searchAll(Specification<ServiceRequest> spec, Sort sort) {
        return requestRepo.findAll(spec, sort);
    }

    // ---------- Lifecycle actions ----------

    /**
     * Assign or re-assign an agent.
     * Accepts either:
     *  - agents.id   (preferred)
     *  - users.id    (fallback; resolve via email)
     *
     * If a UserAccount has role AGENT but no Agent row exists yet, we auto-create it.
     * Allowed while the request is PENDING or ASSIGNED.
     * Locked once IN_PROGRESS / COMPLETED / CANCELLED.
     */
    @Transactional
    public ServiceRequest assign(Long id, Long agentIdOrUserId) {
        ServiceRequest req = getById(id);

        if (req.getStatus() == RequestStatus.IN_PROGRESS
                || req.getStatus() == RequestStatus.COMPLETED
                || req.getStatus() == RequestStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This request is locked; cannot (re)assign after work has started or finished."
            );
        }

        if (agentIdOrUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "agentId is required");
        }

        // Try as agents.id
        Agent agent = agentRepo.findById(agentIdOrUserId).orElse(null);

        // Fallback: treat as users.id → resolve to email → find/create Agent by email
        if (agent == null) {
            UserAccount u = userRepo.findById(agentIdOrUserId).orElse(null);
            if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
                agent = agentRepo.findByEmailIgnoreCase(u.getEmail()).orElse(null);
                if (agent == null) {
                    // If user is agent in account table, create an Agent row on the fly
                    if (u.getRole() == AccountRole.AGENT) {
                        agent = ensureAgentForUser(u);
                    }
                } else if (!agent.isActive() && u.getRole() == AccountRole.AGENT) {
                    // Reactivate if necessary
                    agent.setActive(true);
                    agentRepo.save(agent);
                }
            }
        }

        if (agent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
        }
        if (!agent.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agent is not active");
        }

        // Re-assign allowed while PENDING or ASSIGNED
        req.setAssignedAgent(agent);
        if (req.getStatus() == RequestStatus.PENDING) {
            req.setStatus(RequestStatus.ASSIGNED);
        }

        return requestRepo.save(req);
    }

    @Transactional
    public ServiceRequest start(Long id) {
        ServiceRequest req = getById(id);
        if (req.getStatus() != RequestStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only ASSIGNED requests can be started");
        }
        req.setStatus(RequestStatus.IN_PROGRESS);
        return requestRepo.save(req);
    }

    @Transactional
    public ServiceRequest complete(Long id) {
        ServiceRequest req = getById(id);
        if (req.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only IN_PROGRESS requests can be completed");
        }
        req.setStatus(RequestStatus.COMPLETED);

        Slot slot = req.getSlot();
        if (slot != null && slot.getStatus() == SlotStatus.BOOKED) {
            slot.setStatus(SlotStatus.CONSUMED);
            slotRepo.save(slot);
        }
        return requestRepo.save(req);
    }

    @Transactional
    public ServiceRequest cancel(Long id) {
        ServiceRequest req = getById(id);
        switch (req.getStatus()) {
            case PENDING, ASSIGNED, IN_PROGRESS -> {
                req.setStatus(RequestStatus.CANCELLED);
                Slot slot = req.getSlot();
                if (slot != null && slot.getStatus() == SlotStatus.BOOKED) {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slotRepo.save(slot);
                }
                return requestRepo.save(req);
            }
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already final");
        }
    }

    // ---------- helpers ----------

    /** Ensure there's an Agent row for a given UserAccount (role = AGENT). */
    private Agent ensureAgentForUser(UserAccount u) {
        Agent a = agentRepo.findByEmailIgnoreCase(u.getEmail()).orElse(null);
        if (a == null) {
            a = new Agent();
            a.setFirstName(nullIfBlank(u.getFirstName()));
            a.setLastName(nullIfBlank(u.getLastName()));
            a.setEmail(nullIfBlank(u.getEmail()));
            a.setPhone(nullIfBlank(u.getPhone()));
            a.setActive(true);
        } else {
            // Update stale fields if empty
            if (isBlank(a.getFirstName())) a.setFirstName(nullIfBlank(u.getFirstName()));
            if (isBlank(a.getLastName()))  a.setLastName(nullIfBlank(u.getLastName()));
            if (isBlank(a.getPhone()))     a.setPhone(nullIfBlank(u.getPhone()));
            a.setActive(true);
        }
        return agentRepo.save(a);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
    private static String nullIfBlank(String s) {
        return isBlank(s) ? null : s;
    }

    private static LocalDateTime parseLocalDateTime(String s) {
        // Accept "YYYY-MM-DDTHH:mm" or "YYYY-MM-DDTHH:mm:ss"
        if (s != null && s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
            return LocalDateTime.parse(s + ":00");
        }
        return LocalDateTime.parse(s);
    }

    /**
     * Read scheduledAt from the record even if your record type doesn't currently declare it.
     * (If you added scheduledAt:String to your record, just replace this with return body.scheduledAt();)
     */
    private String tryGetScheduledAt(CreateServiceRequestRequest body) {
        try {
            Method m = body.getClass().getDeclaredMethod("scheduledAt");
            Object v = m.invoke(body);
            return v == null ? null : v.toString();
        } catch (Exception ignore) {
            return null;
        }
    }
}

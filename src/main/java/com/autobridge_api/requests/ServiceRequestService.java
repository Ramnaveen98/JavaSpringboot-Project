package com.autobridge_api.requests;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.agents.AgentRepository;
import com.autobridge_api.requests.dto.RequestDtos.CreateServiceRequestRequest;
import com.autobridge_api.servicecatalog.ServiceOffering;
import com.autobridge_api.servicecatalog.ServiceOfferingRepository;
import com.autobridge_api.slots.Slot;
import com.autobridge_api.slots.SlotRepository;
import com.autobridge_api.slots.SlotStatus;
import com.autobridge_api.vehicles.InventoryVehicle;
import com.autobridge_api.vehicles.InventoryVehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepo;
    private final ServiceOfferingRepository offeringRepo;
    private final InventoryVehicleRepository vehicleRepo;
    private final SlotRepository slotRepo;
    private final AgentRepository agentRepo;

    public ServiceRequestService(ServiceRequestRepository requestRepo,
                                 ServiceOfferingRepository offeringRepo,
                                 InventoryVehicleRepository vehicleRepo,
                                 SlotRepository slotRepo,
                                 AgentRepository agentRepo) {
        this.requestRepo = requestRepo;
        this.offeringRepo = offeringRepo;
        this.vehicleRepo = vehicleRepo;
        this.slotRepo = slotRepo;
        this.agentRepo = agentRepo;
    }


    @Transactional
    public ServiceRequest create(CreateServiceRequestRequest body) {
        ServiceOffering offering = offeringRepo.findById(body.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service offering not found"));

        Slot slot = slotRepo.findById(body.slotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is not available");
        }

        InventoryVehicle vehicle = null;
        if (body.inventoryVehicleId() != null) {
            vehicle = vehicleRepo.findById(body.inventoryVehicleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory vehicle not found"));
        }

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

        // Book slot
        slot.setStatus(SlotStatus.BOOKED);

        requestRepo.save(req);
        slotRepo.save(slot);

        return req;
    }

    @Transactional(readOnly = true)
    public ServiceRequest getById(Long id) {
        return requestRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    // ---------- Listing (existing) ----------
    @Transactional(readOnly = true)
    public Page<ServiceRequest> listAll(Pageable pageable) {
        return requestRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequest> listByStatus(RequestStatus status, Pageable pageable) {
        return requestRepo.findAll((root, q, cb) -> cb.equal(root.get("status"), status), pageable);
    }

    // ---------- NEW: Admin search helpers ----------
    @Transactional(readOnly = true)
    public Page<ServiceRequest> search(Specification<ServiceRequest> spec, Pageable pageable) {
        return requestRepo.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequest> searchAll(Specification<ServiceRequest> spec, Sort sort) {
        return requestRepo.findAll(spec, sort);
    }

    // ---------- Lifecycle ----------
    @Transactional
    public ServiceRequest assign(Long id, Long agentId) {
        ServiceRequest req = getById(id);

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING requests can be assigned");
        }
        if (agentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "agentId is required");
        }

        Agent agent = agentRepo.findById(agentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
        if (!agent.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agent is not active");
        }

        req.setAssignedAgent(agent);
        req.setStatus(RequestStatus.ASSIGNED);
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
        if (slot.getStatus() == SlotStatus.BOOKED) {
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
                if (slot.getStatus() == SlotStatus.BOOKED) {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slotRepo.save(slot);
                }
                return requestRepo.save(req);
            }
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already final");
        }
    }
}

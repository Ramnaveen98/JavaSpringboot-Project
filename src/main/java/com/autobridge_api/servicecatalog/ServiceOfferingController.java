/*
// src/main/java/com/autobridge_api/servicecatalog/ServiceOfferingController.java
package com.autobridge_api.servicecatalog;

import com.autobridge_api.servicecatalog.dto.ServiceDtos.ServiceOfferingDto;
import com.autobridge_api.servicecatalog.dto.ServiceDtos.UpsertServiceOfferingRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Admin-facing CRUD for Service Offerings.
 * Public read-only listing should use ServiceListController (/api/v1/public/services).

@RestController
@RequestMapping("/api/v1/admin/services")
public class ServiceOfferingController {

    private final ServiceOfferingRepository repo;

    public ServiceOfferingController(ServiceOfferingRepository repo) {
        this.repo = repo;
    }

    /** List all or only active, sorted by name (when active=true).
    @GetMapping
    public ResponseEntity<List<ServiceOfferingDto>> list(@RequestParam(required = false) Boolean active) {
        final List<ServiceOffering> src = (active != null && active)
                ? repo.findActiveSorted()
                : repo.findAll();
        final var dtos = src.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOfferingDto> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(s -> ResponseEntity.ok(toDto(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ServiceOfferingDto> bySlug(@PathVariable String slug) {
        return repo.findBySlug(slug)
                .map(s -> ResponseEntity.ok(toDto(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ServiceOfferingDto> create(@Valid @RequestBody UpsertServiceOfferingRequest req) {
        if (repo.existsBySlug(req.slug())) {
            // 422: duplicate slug
            return ResponseEntity.unprocessableEntity().<ServiceOfferingDto>build();
        }
        var s = ServiceOffering.builder()
                .slug(req.slug())
                .name(req.name())
                .description(req.description())
                .basePrice(req.basePrice())
                .durationMinutes(req.durationMinutes())
                .active(req.active() == null || Boolean.TRUE.equals(req.active()))
                .build();

        s = repo.save(s);
        return ResponseEntity.created(URI.create("/api/v1/admin/services/" + s.getId()))
                .body(toDto(s));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOfferingDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpsertServiceOfferingRequest req) {
        return repo.findById(id).map(existing -> {
            // If slug is changing, ensure uniqueness
            if (req.slug() != null && !req.slug().equals(existing.getSlug())
                    && repo.existsBySlug(req.slug())) {
                // 👇 Add the generic to keep the method return type consistent
                return ResponseEntity.unprocessableEntity().<ServiceOfferingDto>build();
            }
            if (req.slug() != null) existing.setSlug(req.slug());
            if (req.name() != null) existing.setName(req.name());
            existing.setDescription(req.description()); // nullable ok
            if (req.basePrice() != null) existing.setBasePrice(req.basePrice());
            if (req.durationMinutes() != null) existing.setDurationMinutes(req.durationMinutes());
            if (req.active() != null) existing.setActive(req.active());

            var saved = repo.save(existing);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        try {
            repo.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            // Service is referenced by requests/slots etc → 409 Conflict
            return ResponseEntity.status(409).build();
        }
    }

    private ServiceOfferingDto toDto(ServiceOffering s) {
        return new ServiceOfferingDto(
                s.getId(),
                s.getSlug(),
                s.getName(),
                s.getDescription(),
                s.getBasePrice(),
                s.getDurationMinutes(),
                s.isActive()
        );
    }
}
*/


// src/main/java/com/autobridge_api/servicecatalog/ServiceOfferingController.java
package com.autobridge_api.servicecatalog;

import com.autobridge_api.servicecatalog.dto.ServiceDtos.ServiceOfferingDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PUBLIC read-only endpoints for services.
 * All admin (create/update/delete) is in ServiceAdminController under /api/v1/admin/services.
 */
@RestController
@RequestMapping("/api/v1/services")
public class ServiceOfferingController {

    private final ServiceOfferingRepository repo;

    public ServiceOfferingController(ServiceOfferingRepository repo) {
        this.repo = repo;
    }

    /** List services. If active=true, filter active; otherwise return all. */
    @GetMapping
    public List<ServiceOfferingDto> list(@RequestParam(required = false) Boolean active) {
        var list = (active != null && active)
                ? repo.findByActiveTrueOrderByNameAsc()
                : repo.findAll();
        return list.stream().map(this::toDto).toList();
    }

    /** Get by id (public). */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceOfferingDto> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(s -> ResponseEntity.ok(toDto(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get by slug (public). */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ServiceOfferingDto> bySlug(@PathVariable String slug) {
        return repo.findBySlug(slug)
                .map(s -> ResponseEntity.ok(toDto(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    /*  IMPORTANT:
        NO admin endpoints here. Do NOT add POST/PUT/DELETE in this controller.
        Admin endpoints live in ServiceAdminController at /api/v1/admin/services.
     */

    private ServiceOfferingDto toDto(ServiceOffering s) {
        return new ServiceOfferingDto(
                s.getId(),
                s.getSlug(),
                s.getName(),
                s.getDescription(),
                s.getBasePrice(),
                s.getDurationMinutes(),
                s.isActive()
        );
    }
}


package com.autobridge_api.servicecatalog;

import com.autobridge_api.servicecatalog.dto.ServiceDtos.ServiceOfferingDto;
import com.autobridge_api.servicecatalog.dto.ServiceDtos.UpsertServiceOfferingRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/services")
public class ServiceAdminController {

    private final ServiceOfferingRepository repo;

    public ServiceAdminController(ServiceOfferingRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<ServiceOfferingDto>> list() {
        var out = repo.findAll().stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<ServiceOfferingDto> create(@Valid @RequestBody UpsertServiceOfferingRequest body) {
        if (repo.existsBySlug(body.slug())) {
            // 👇 make the builder generic to match the method's return type
            return ResponseEntity.unprocessableEntity().<ServiceOfferingDto>build();
        }
        var s = ServiceOffering.builder()
                .slug(body.slug())
                .name(body.name())
                .description(body.description())
                .basePrice(body.basePrice() != null ? body.basePrice() : BigDecimal.ZERO)
                .durationMinutes(body.durationMinutes() != null ? body.durationMinutes() : 60)
                .active(body.active() == null || body.active())
                .build();
        s = repo.save(s);
        return ResponseEntity.created(URI.create("/api/v1/admin/services/" + s.getId()))
                .body(toDto(s));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOfferingDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpsertServiceOfferingRequest body) {
        return repo.findById(id).map(existing -> {
            if (!existing.getSlug().equals(body.slug()) && repo.existsBySlug(body.slug())) {
                // 👇 same fix here
                return ResponseEntity.unprocessableEntity().<ServiceOfferingDto>build();
            }
            existing.setSlug(body.slug());
            existing.setName(body.name());
            existing.setDescription(body.description());
            if (body.basePrice() != null) existing.setBasePrice(body.basePrice());
            if (body.durationMinutes() != null) existing.setDurationMinutes(body.durationMinutes());
            if (body.active() != null) existing.setActive(body.active());
            var saved = repo.save(existing);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        try {
            repo.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "CONFLICT",
                    "message", "This service is referenced by other data (requests/slots). Deactivate it instead."
            ));
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

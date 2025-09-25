package com.autobridge_api.vehicles;

import com.autobridge_api.vehicles.dto.VehiclesDtos.InventoryVehicleDto;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryVehicleRepository repo;

    public InventoryController(InventoryVehicleRepository repo) {
        this.repo = repo;
    }

    /** Paged inventory list with optional filters */
    @GetMapping
    public Page<InventoryVehicleDto> list(
            @RequestParam(required = false) Long makeId,
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<InventoryVehicle> spec = Specification.where(null);

        if (makeId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("make").get("id"), makeId));
        }
        if (modelId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("model").get("id"), modelId));
        }
        if (status != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }
        if (minPrice != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("vin")), like),
                    cb.like(cb.lower(root.get("color")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("model").get("name")), like),
                    cb.like(cb.lower(root.get("make").get("name")), like)
            ));
        }

        return repo.findAll(spec, pageable).map(this::toDto);
    }

    /** Single vehicle detail */
    @GetMapping("/{id}")
    public ResponseEntity<InventoryVehicleDto> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(v -> ResponseEntity.ok(toDto(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    private InventoryVehicleDto toDto(InventoryVehicle v) {
        return new InventoryVehicleDto(
                v.getId(),
                v.getMake().getId(),
                v.getMake().getName(),
                v.getModel().getId(),
                v.getModel().getName(),
                v.getYear(),
                v.getVin(),
                v.getColor(),
                v.getPrice(),
                v.getStatus(),
                v.getImageUrl(),
                v.getDescription()
        );
    }
}

package com.autobridge_api.vehicles;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class InventoryController {

    private final InventoryVehicleRepository repo;

    public InventoryController(InventoryVehicleRepository repo) {
        this.repo = repo;
    }

    /** Public DTO (strings & numbers only; no nested objects). */
    public record VehicleDto(
            Long id,
            String vin,
            String title,
            String brand,
            String model,
            String color,
            Integer year,
            BigDecimal price,
            String status,
            String imageUrl,
            String description
    ) {}

    /** GET /api/v1/vehicles/public — searchable public catalog. */
    @GetMapping("/public")
    public Page<VehicleDto> searchPublic(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "make", required = false) String makeAlias,   // alias for brand
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "year", required = false) Integer year,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,DESC") String sort
    ) {
        String brandParam = StringUtils.hasText(brand) ? brand : makeAlias;

        Specification<InventoryVehicle> spec = Specification.where(null);

        if (StringUtils.hasText(q)) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("brand")), like),
                    cb.like(cb.lower(root.get("model")), like),
                    cb.like(cb.lower(root.get("color")), like),
                    cb.like(cb.lower(root.get("description")), like)
            ));
        }

        if (StringUtils.hasText(brandParam)) {
            String like = "%" + brandParam.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("brand")), like));
        }

        if (StringUtils.hasText(model)) {
            String like = "%" + model.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("model")), like));
        }

        if (StringUtils.hasText(status)) {
            try {
                InventoryStatus st = InventoryStatus.valueOf(status.trim().toUpperCase());
                spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), st));
            } catch (IllegalArgumentException ignored) {
                // unknown status -> ignore filter
            }
        }

        if (year != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("year"), year));
        }

        if (minPrice != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100), sortObj);

        Page<InventoryVehicle> pg = repo.findAll(spec, pageable);
        return pg.map(this::toDto);
    }

    /** GET /api/v1/vehicles/public/{id} — single vehicle for details page. */
    @GetMapping("/public/{id}")
    public ResponseEntity<VehicleDto> getOne(@PathVariable Long id) {
        return repo.findById(id)
                .map(v -> ResponseEntity.ok(toDto(v)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /* ===================== helpers ===================== */

    private VehicleDto toDto(InventoryVehicle v) {
        return new VehicleDto(
                v.getId(),
                v.getVin(),
                v.getTitle(),
                v.getBrand(),       // alias for "make"
                v.getModel(),
                v.getColor(),
                v.getYear(),
                v.getPrice(),
                v.getStatus() != null ? v.getStatus().name() : null,
                v.getImageUrl(),
                v.getDescription()
        );
    }

    private Sort parseSort(String s) {
        if (!StringUtils.hasText(s)) return Sort.by(Sort.Order.desc("createdAt"));
        try {
            // Format: "field,ASC|DESC;field2,ASC|DESC"
            String[] parts = s.split("[;]");
            List<Sort.Order> orders = new ArrayList<>();
            for (String p : parts) {
                String[] duo = p.trim().split(",");
                String field = duo[0].trim();
                Sort.Direction dir = (duo.length > 1 && "ASC".equalsIgnoreCase(duo[1].trim()))
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                orders.add(new Sort.Order(dir, field));
            }
            return orders.isEmpty() ? Sort.by(Sort.Order.desc("createdAt")) : Sort.by(orders);
        } catch (Exception e) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }
    }
}

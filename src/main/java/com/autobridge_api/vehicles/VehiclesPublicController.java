package com.autobridge_api.vehicles;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/vehicles") // public listing
@Transactional(readOnly = true)
public class VehiclesPublicController {

    private final InventoryVehicleRepository repo;

    public VehiclesPublicController(InventoryVehicleRepository repo) {
        this.repo = repo;
    }

    // DTO so the JSON key is imageUrl (camelCase) and we avoid lazy pitfalls
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

    @GetMapping
    public Page<VehicleDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String q
    ) {
        Specification<InventoryVehicle> spec = Specification.where(null);

        if (StringUtils.hasText(brand)) {
            String like = "%" + brand.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("brand")), like));
        }
        if (StringUtils.hasText(model)) {
            String like = "%" + model.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("model")), like));
        }
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

        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 200), s);

        Page<InventoryVehicle> pg = repo.findAll(spec, pageable);
        return pg.map(this::toDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDto> getOne(@PathVariable Long id) {
        return repo.findById(id)
                .map(v -> ResponseEntity.ok(toDto(v)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private VehicleDto toDto(InventoryVehicle v) {
        return new VehicleDto(
                v.getId(),
                v.getVin(),
                v.getTitle(),
                v.getBrand(),
                v.getModel(),
                v.getColor(),
                v.getYear(),
                v.getPrice(),
                v.getStatus() != null ? v.getStatus().name() : null,
                v.getImage_url(), // map snake -> camel
                v.getDescription()
        );
    }

    private Sort parseSort(String s) {
        if (!StringUtils.hasText(s)) return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        try {
            String[] parts = s.split("[;]");
            List<Sort.Order> orders = new ArrayList<>();
            for (String p : parts) {
                String[] duo = p.trim().split(",");
                String field = duo[0].trim();
                Sort.Direction dir = (duo.length > 1 && "ASC".equalsIgnoreCase(duo[1].trim()))
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                orders.add(new Sort.Order(dir, field));
            }
            return orders.isEmpty() ? Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")) : Sort.by(orders);
        } catch (Exception e) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }
    }
}

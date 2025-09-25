package com.autobridge_api.servicecatalog;

import com.autobridge_api.servicecatalog.dto.ServiceDtos.ServiceOfferingDto;
import com.autobridge_api.servicecatalog.dto.ServiceDtos.UpsertServiceOfferingRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceOfferingController {

    private final ServiceOfferingRepository repo;

    public ServiceOfferingController(ServiceOfferingRepository repo) {
        this.repo = repo;
    }


    @GetMapping
    public List<ServiceOfferingDto> list(@RequestParam(required = false) Boolean active) {
        var list = (active != null && active)
                ? repo.findByActiveTrueOrderByNameAsc()
                : repo.findAll();
        return list.stream().map(this::toDto).toList();
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
            return ResponseEntity.unprocessableEntity().build(); // 422: duplicate slug
        }
        var s = ServiceOffering.builder()
                .slug(req.slug())
                .name(req.name())
                .description(req.description())
                .basePrice(req.basePrice())
                .durationMinutes(req.durationMinutes())
                .active(req.active() == null ? true : req.active())
                .build();

        s = repo.save(s);
        return ResponseEntity.created(URI.create("/api/v1/services/" + s.getId()))
                .body(toDto(s));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ServiceOfferingDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpsertServiceOfferingRequest req) {
        return repo.findById(id).map(existing -> {
            existing.setSlug(req.slug());
            existing.setName(req.name());
            existing.setDescription(req.description());
            existing.setBasePrice(req.basePrice());
            existing.setDurationMinutes(req.durationMinutes());
            if (req.active() != null) existing.setActive(req.active());
            return ResponseEntity.ok(toDto(repo.save(existing)));
        }).orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ServiceOfferingDto toDto(ServiceOffering s) {
        return new ServiceOfferingDto(
                s.getId(), s.getSlug(), s.getName(), s.getDescription(),
                s.getBasePrice(), s.getDurationMinutes(), s.isActive()
        );
    }
}

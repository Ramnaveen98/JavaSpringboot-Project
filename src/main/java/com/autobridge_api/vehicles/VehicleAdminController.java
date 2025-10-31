package com.autobridge_api.vehicles;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
@PreAuthorize("hasRole('ADMIN')")
public class VehicleAdminController {

    public record UpsertDto(
            Long id,
            String vin,
            String title,
            String brand,
            String model,       // ← now supported
            String color,       // ← now supported
            Integer year,
            BigDecimal price,
            String status,
            String imageUrl,
            String description,
            // legacy aliases the UI may send
            String name,
            String make
    ) {}

    private final InventoryVehicleRepository repo;
    private final VehicleImageStorage storage;

    public VehicleAdminController(InventoryVehicleRepository repo, VehicleImageStorage storage) {
        this.repo = repo;
        this.storage = storage;
    }

    /* ---------- CRUD ---------- */

    @GetMapping
    @Transactional(readOnly = true)
    public List<InventoryVehicle> list(
            @RequestParam(value = "sort", defaultValue = "createdAt,DESC") String sort) {
        Sort s = "id,ASC".equalsIgnoreCase(sort)
                ? Sort.by("id").ascending()
                : Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return repo.findAll(s);
    }

    @PostMapping
    public ResponseEntity<InventoryVehicle> create(@RequestBody UpsertDto dto) {
        var v = new InventoryVehicle();
        v.setVin(blankToNull(dto.vin()));
        v.setTitle(nn(dto.title(), dto.name()));
        v.setBrand(nn(dto.brand(), dto.make()));
        v.setModel(blankToNull(dto.model()));
        v.setColor(blankToNull(dto.color()));                   // ← save color
        v.setYear(dto.year());
        v.setPrice(normPrice(dto.price()));
        v.setStatus(parseStatus(dto.status()));
        v.setDescription(dto.description());
        if (notBlank(dto.imageUrl())) v.setImage_url(dto.imageUrl().trim());
        validate(v);
        var saved = repo.save(v);
        return ResponseEntity.created(URI.create("/api/v1/admin/vehicles/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public InventoryVehicle update(@PathVariable Long id, @RequestBody UpsertDto dto) {
        var v = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        if (dto.vin()   != null) v.setVin(blankToNull(dto.vin()));
        if (notBlank(dto.title()) || notBlank(dto.name()))  v.setTitle(nn(dto.title(), dto.name()));
        if (notBlank(dto.brand()) || notBlank(dto.make()))  v.setBrand(nn(dto.brand(), dto.make()));
        if (dto.model() != null) v.setModel(blankToNull(dto.model()));
        if (dto.color() != null) v.setColor(blankToNull(dto.color()));      // ← update color
        if (dto.year()  != null) v.setYear(dto.year());
        if (dto.price() != null) v.setPrice(normPrice(dto.price()));
        if (dto.status()!= null) v.setStatus(parseStatus(dto.status()));
        if (dto.imageUrl() != null) v.setImage_url(dto.imageUrl().trim());
        if (dto.description() != null) v.setDescription(dto.description());
        validate(v);
        return repo.save(v);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        storage.deleteAllForVehicle(id); // best-effort cleanup
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* ---------- image by URL ---------- */

    public record ImageUrlDto(String imageUrl) {}

    @PutMapping("/{id}/image")
    public InventoryVehicle setImageUrl(@PathVariable Long id, @RequestBody ImageUrlDto dto) {
        var v = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        String url = (dto.imageUrl() == null) ? "" : dto.imageUrl().trim();
        v.setImage_url(url);
        return repo.save(v);
    }

    /* ---------- image upload (multipart) ---------- */

    @PostMapping(path = "/{id}/image-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InventoryVehicle uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file uploaded");
        var v = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        String publicUrl = storage.saveVehicleImage(id, file);
        if (StringUtils.hasText(publicUrl)) {
            v.setImage_url(publicUrl.trim());
        }
        return repo.save(v); // persist image_url immediately
    }

    /* ---------- helpers ---------- */

    private static String nn(String... options) {
        for (String s : options) if (notBlank(s)) return s.trim();
        return null;
    }
    private static boolean notBlank(String s) { return s != null && !s.trim().isBlank(); }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    private static BigDecimal normPrice(BigDecimal v) {
        if (v == null) return null;
        return v.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static void validate(InventoryVehicle v) {
        if (!StringUtils.hasText(v.getTitle())) throw new IllegalArgumentException("title is required");
        if (!StringUtils.hasText(v.getBrand())) throw new IllegalArgumentException("brand is required");
    }

    private static InventoryStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return InventoryStatus.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return null; }
    }
}

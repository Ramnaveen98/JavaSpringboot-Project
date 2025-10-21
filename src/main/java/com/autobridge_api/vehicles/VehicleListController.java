package com.autobridge_api.vehicles;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class VehicleListController {

    private final VehicleListRepository repo;

    public VehicleListController(VehicleListRepository repo) {
        this.repo = repo;
    }

    public record VehicleDto(Long id, String title, String brand, Double price, String imageUrl) {
        static VehicleDto from(VehicleListRepository.VehicleRow r) {
            return new VehicleDto(r.getId(), r.getTitle(), r.getBrand(), r.getPrice(), r.getImageUrl());
        }
    }

    /** Authenticated list (keeps your existing security rules) */
    @GetMapping("/vehicles")
    @Transactional(readOnly = true)
    public List<VehicleDto> listProtected() {
        return repo.listAllRows().stream().map(VehicleDto::from).toList();
    }

    /** Public list (no token) — handy for the public Cars page */
    @GetMapping("/public/vehicles")
    @Transactional(readOnly = true)
    public List<VehicleDto> listPublic() {
        return repo.listAllRows().stream().map(VehicleDto::from).toList();
    }
}

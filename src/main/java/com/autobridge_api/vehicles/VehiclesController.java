package com.autobridge_api.vehicles;

import com.autobridge_api.vehicles.dto.VehiclesDtos.VehicleMakeDto;
import com.autobridge_api.vehicles.dto.VehiclesDtos.VehicleModelDto;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehiclesController {

    private final VehicleMakeRepository makeRepo;
    private final VehicleModelRepository modelRepo;

    public VehiclesController(VehicleMakeRepository makeRepo, VehicleModelRepository modelRepo) {
        this.makeRepo = makeRepo;
        this.modelRepo = modelRepo;
    }

    /** List all makes sorted by name (e.g., Toyota, Honda, Tesla) */
    @GetMapping("/makes")
    public List<VehicleMakeDto> makes() {
        return makeRepo.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(m -> new VehicleMakeDto(m.getId(), m.getName()))
                .toList();
    }

    /** List models for a given make (e.g., Camry, Corolla for Toyota) */
    @GetMapping("/models")
    public ResponseEntity<List<VehicleModelDto>> models(@RequestParam Long makeId) {
        if (!makeRepo.existsById(makeId)) {
            return ResponseEntity.notFound().build();
        }
        var list = modelRepo.findByMakeIdOrderByNameAsc(makeId).stream()
                .map(m -> new VehicleModelDto(m.getId(), m.getMake().getId(), m.getName()))
                .toList();
        return ResponseEntity.ok(list);
    }
}

package com.autobridge_api.vehicles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    List<VehicleModel> findByMakeIdOrderByNameAsc(Long makeId);
    Optional<VehicleModel> findByMakeIdAndNameIgnoreCase(Long makeId, String name);
    boolean existsByMakeIdAndNameIgnoreCase(Long makeId, String name);
}

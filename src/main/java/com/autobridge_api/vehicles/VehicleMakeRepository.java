package com.autobridge_api.vehicles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleMakeRepository extends JpaRepository<VehicleMake, Long> {
    Optional<VehicleMake> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}

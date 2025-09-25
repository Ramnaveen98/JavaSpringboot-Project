package com.autobridge_api.vehicles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
public interface InventoryVehicleRepository
        extends JpaRepository<InventoryVehicle, Long>, JpaSpecificationExecutor<InventoryVehicle> {
    Optional<InventoryVehicle> findByVin(String vin);
    // We’ll use Specifications for flexible searching (make/model/status/q, paging)
}

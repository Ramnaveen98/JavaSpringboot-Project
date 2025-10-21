/*package com.autobridge_api.vehicles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
public interface InventoryVehicleRepository
        extends JpaRepository<InventoryVehicle, Long>, JpaSpecificationExecutor<InventoryVehicle> {
    Optional<InventoryVehicle> findByVin(String vin);
    // We’ll use Specifications for flexible searching (make/model/status/q, paging)
}
*/

package com.autobridge_api.vehicles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JPA repository for InventoryVehicle with Specification support.
 * Extending JpaSpecificationExecutor enables:
 *   findAll(Specification<T> spec)
 *   findAll(Specification<T> spec, Pageable pageable)
 *   count(Specification<T> spec), etc.
 */
public interface InventoryVehicleRepository
        extends JpaRepository<InventoryVehicle, Long>,
        JpaSpecificationExecutor<InventoryVehicle> {

    /** For seeders/upserts that key on VIN. VIN may be null; when present, it is unique. */
    Optional<InventoryVehicle> findByVin(String vin);
}

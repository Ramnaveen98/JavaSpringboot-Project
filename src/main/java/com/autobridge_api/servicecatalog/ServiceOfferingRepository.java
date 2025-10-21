/*package com.autobridge_api.servicecatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    Optional<ServiceOffering> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<ServiceOffering> findByActiveTrueOrderByNameAsc();
}*/


// src/main/java/com/autobridge_api/servicecatalog/ServiceOfferingRepository.java
// src/main/java/com/autobridge_api/servicecatalog/ServiceOfferingRepository.java
// src/main/java/com/autobridge_api/servicecatalog/ServiceOfferingRepository.java


package com.autobridge_api.servicecatalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    Optional<ServiceOffering> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Name used by the current controller (kept for compatibility)
    List<ServiceOffering> findByActiveTrueOrderByNameAsc();

    // Also keep the explicit JPQL version if you want to use it elsewhere
    @Query("select s from ServiceOffering s where s.active = true order by s.name asc")
    List<ServiceOffering> findActiveOrderByName();
}

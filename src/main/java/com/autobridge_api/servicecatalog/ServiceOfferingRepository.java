package com.autobridge_api.servicecatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    Optional<ServiceOffering> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<ServiceOffering> findByActiveTrueOrderByNameAsc();
}

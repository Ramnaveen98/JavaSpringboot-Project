// src/main/java/com/autobridge_api/services/ServiceListRepository.java
package com.autobridge_api.services;

import com.autobridge_api.servicecatalog.ServiceOffering;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only repository that queries ServiceOffering but projects to a lightweight row.
 * NOTE: The repository is typed to the managed entity (ServiceOffering),
 * NOT the projection interface. That avoids the "Not a managed type" error.
 */
@Component
public interface ServiceListRepository extends Repository<ServiceOffering, Long> {

    /**
     * Public list of active services, sorted by name (adjust column names if your schema differs).
     * If your DB stores booleans as TINYINT(1), keep "= 1". If it's a proper BOOLEAN, "= true" also works.
     */
    @Query(
            value = """
                SELECT
                  id,
                  slug,
                  name,
                  duration_minutes AS durationMinutes,
                  base_price       AS basePrice
                FROM service_offering
                WHERE active = 1
                ORDER BY name
                """,
            nativeQuery = true
    )
    List<ServiceRow> findPublic();

    /**
     * Projection for the public list. Keep method names matching the selected aliases.
     */
    interface ServiceRow {
        Long getId();
        String getSlug();
        String getName();
        Integer getDurationMinutes();
        BigDecimal getBasePrice();
    }
}

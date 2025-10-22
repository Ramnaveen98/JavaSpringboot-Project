// src/main/java/com/autobridge_api/services/ServiceListController.java
package com.autobridge_api.services;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
public class ServiceListController {

    private final ServiceListRepository repo;

    public ServiceListController(ServiceListRepository repo) {
        this.repo = repo;
    }

    public record ServiceDto(
            Long id,
            String slug,
            String name,
            Integer durationMinutes,
            BigDecimal basePrice
    ) {
        static ServiceDto from(ServiceListRepository.ServiceRow r) {
            return new ServiceDto(r.getId(), r.getSlug(), r.getName(), r.getDurationMinutes(), r.getBasePrice());
        }
    }

    /** Public list (no login). */
    @GetMapping("/services")
    @Transactional(readOnly = true)
    public List<ServiceDto> listPublic() {
        return repo.findPublic().stream().map(ServiceDto::from).toList();
    }
}

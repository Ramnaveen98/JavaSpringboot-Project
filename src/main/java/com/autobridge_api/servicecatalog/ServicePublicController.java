package com.autobridge_api.servicecatalog;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServicePublicController {

    private final ServiceOfferingRepository repo;

    public record PublicServiceDto(
            Long id,
            String slug,
            String name,
            Integer durationMinutes,
            BigDecimal basePrice
    ) {
        static PublicServiceDto of(ServiceOffering s) {
            return new PublicServiceDto(
                    s.getId(),
                    s.getSlug(),
                    s.getName(),
                    s.getDurationMinutes(),
                    s.getBasePrice()
            );
        }
    }

    // Anonymous-friendly public list
    @GetMapping("/public")
    public ResponseEntity<List<PublicServiceDto>> publicList() {
        var list = repo.findActiveOrderByName()
                .stream()
                .map(PublicServiceDto::of)
                .toList();
        return ResponseEntity.ok(list);
    }
}

package com.autobridge_api.vehicles;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "inventory_vehicle",
        uniqueConstraints = @UniqueConstraint(columnNames = "vin")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "make_id", nullable = false)
    private VehicleMake make;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private VehicleModel model;

    // Basic attributes
    @Column(nullable = false)
    private Integer year;            // Year

    @Column(nullable = false, length = 17, unique = true)
    private String vin;              // Vehicle Identification Number

    @Column(length = 32)
    private String color;            //  color

    @Column(precision = 12, scale = 2)
    private BigDecimal price;        // Asking price

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 512)
    private String imageUrl;         // optional hero image for cards

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

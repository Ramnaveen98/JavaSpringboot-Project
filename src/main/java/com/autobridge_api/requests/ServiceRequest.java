package com.autobridge_api.requests;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.servicecatalog.ServiceOffering;
import com.autobridge_api.slots.Slot;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// NEW: tolerate missing vehicle row without throwing
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "service_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----- relations -----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    // IMPORTANT: if the vehicle row was deleted, treat it as null instead of throwing
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_vehicle_id")
    private com.autobridge_api.vehicles.InventoryVehicle inventoryVehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private Agent assignedAgent;

    // ----- status -----

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RequestStatus status;

    // ----- contact -----

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "user_first_name")
    private String userFirstName;

    @Column(name = "user_last_name", nullable = false)
    private String userLastName;

    @Column(name = "user_phone")
    private String userPhone;

    // ----- address -----

    @Column(name = "addr_line1", nullable = false)
    private String addressLine1;

    @Column(name = "addr_line2")
    private String addressLine2;

    @Column(name = "addr_city", nullable = false)
    private String city;

    @Column(name = "addr_state")
    private String state;

    @Column(name = "addr_postal_code")
    private String postalCode;

    @Column(name = "addr_country")
    private String country;

    // ----- misc -----

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ----- lifecycle guards (make NOT NULL columns safe for local testing) -----

    @PrePersist
    void prePersist() {
        if (this.status == null) this.status = RequestStatus.PENDING;
        final Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        // Safety defaults to avoid DB integrity 409s during UI wiring
        if (this.userLastName == null || this.userLastName.isBlank()) this.userLastName = "N/A";
        if (this.userEmail == null || this.userEmail.isBlank()) this.userEmail = "no-email@local";
        if (this.addressLine1 == null || this.addressLine1.isBlank()) this.addressLine1 = "N/A";
        if (this.city == null || this.city.isBlank()) this.city = "N/A";
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

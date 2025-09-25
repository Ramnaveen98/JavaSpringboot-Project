package com.autobridge_api.requests;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.servicecatalog.ServiceOffering;
import com.autobridge_api.slots.Slot;
import com.autobridge_api.vehicles.InventoryVehicle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "service_request",
        indexes = {
                @Index(name = "idx_request_status", columnList = "status"),
                @Index(name = "idx_request_email",  columnList = "user_email"),
                @Index(name = "idx_request_agent",  columnList = "assigned_agent_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "slot_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Assigned agent (nullable until assigned) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private Agent assignedAgent;

    /** Which catalog service this request is for (Test Drive / Delivery / Service item) */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    /** Optional: which inventory vehicle (for test drive or delivery) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_vehicle_id")
    private InventoryVehicle inventoryVehicle;

    /** The booked time window (1:1 when capacity = 1) */
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private Slot slot;

    /** Lifecycle */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RequestStatus status = RequestStatus.PENDING;

    /** Contact info captured at request creation */
    @Column(name = "user_first_name", length = 64, nullable = false)
    private String userFirstName;

    @Column(name = "user_last_name", length = 64, nullable = false)
    private String userLastName;

    @Column(name = "user_email", length = 128, nullable = false)
    private String userEmail;

    @Column(name = "user_phone", length = 32)
    private String userPhone;

    /** Basic address (country not restricted to US) */
    @Column(name = "addr_line1", length = 128, nullable = false)
    private String addressLine1;

    @Column(name = "addr_line2", length = 128)
    private String addressLine2;

    @Column(name = "addr_city", length = 64, nullable = false)
    private String city;

    @Column(name = "addr_state", length = 64)
    private String state;

    @Column(name = "addr_postal_code", length = 32)
    private String postalCode;

    @Column(name = "addr_country", length = 64)
    private String country;

    /** Extra notes from user */
    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

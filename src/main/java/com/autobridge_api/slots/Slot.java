package com.autobridge_api.slots;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "slots",
        indexes = {
                @Index(name = "idx_slots_type_start", columnList = "type,start_at"),
                @Index(name = "idx_slots_status", columnList = "status"),
                @Index(name = "idx_slots_agent", columnList = "agent_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotType type;


    @Column(name = "start_at", nullable = false)
    private Instant startAt;


    @Column(name = "end_at", nullable = false)
    private Instant endAt;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status = SlotStatus.AVAILABLE;


    @Column(name = "agent_id")
    private Long agentId;


    @Column(nullable = false)
    private Integer capacity = 1;


    @Column(length = 512)
    private String notes;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

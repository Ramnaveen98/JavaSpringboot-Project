package com.autobridge_api.agents;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "agents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) private String firstName;
    @Column(nullable=false) private String lastName;

    @Column(nullable=false, unique=true, length=255)
    private String email;

    @Column(nullable=true, length=32)
    private String phone;

    @Column(nullable=false)
    private boolean active = true;

    @Column(nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}

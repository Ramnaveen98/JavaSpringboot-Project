package com.autobridge_api.feedback;

import com.autobridge_api.requests.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "feedback",
        uniqueConstraints = {
                // exactly one feedback per request
                @UniqueConstraint(columnNames = "request_id")
        },
        indexes = {
                @Index(name = "idx_feedback_request", columnList = "request_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private ServiceRequest request;

    @Column(nullable = false)
    private Integer rating; // 1..5

    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

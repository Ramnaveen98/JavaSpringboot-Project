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
        // Enforce ONE feedback per request via a unique constraint on request_id
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_feedback_request", columnNames = {"request_id"})
        },
        // Keep the index (it coexists fine with the unique constraint)
        indexes = {
                @Index(name = "idx_feedback_request", columnList = "request_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One feedback per request (enforced by unique constraint on request_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest request;

    @Column(nullable = false)
    private Integer rating; // 1..5

    // Controller validates non-empty; left nullable=true to avoid breaking existing rows
    @Column(length = 2000)
    private String comment;

    // Who left the feedback (optional; add nullable=false if you want to enforce)
    @Column(name = "author_email", length = 255)
    private String authorEmail;

    /** ---- Admin acknowledgement status ---- */
    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged = false;

    @Column(name = "acknowledged_by", length = 255)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

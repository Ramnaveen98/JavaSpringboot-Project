package com.autobridge_api.feedbackadmin;

import jakarta.persistence.*;

@Entity
@Table(name = "feedback_admin_meta", uniqueConstraints = @UniqueConstraint(columnNames = "feedback_id"))
public class FeedbackAdminMeta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feedback_id", nullable = false, unique = true)
    private Long feedbackId;

    @Column(nullable = false)
    private boolean acknowledged = false;

    public FeedbackAdminMeta() {}
    public FeedbackAdminMeta(Long id, Long feedbackId, boolean acknowledged) {
        this.id = id; this.feedbackId = feedbackId; this.acknowledged = acknowledged;
    }

    public static FeedbackAdminMeta of(Long feedbackId, boolean acknowledged) {
        return new FeedbackAdminMeta(null, feedbackId, acknowledged);
    }

    public Long getId() { return id; }
    public Long getFeedbackId() { return feedbackId; }
    public boolean isAcknowledged() { return acknowledged; }

    public void setId(Long id) { this.id = id; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
}

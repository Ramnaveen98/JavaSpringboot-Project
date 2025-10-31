package com.autobridge_api.auth.reset;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "password_reset_otp",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_pwdreset_active",
                        columnNames = {"email", "purpose", "used"}
                )
        }
)
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false, length = 32)
    private String purpose; // 'SELF_RESET' | 'ADMIN_RESET'

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "requested_by", length = 255)
    private String requestedBy; // admin email for admin resets (nullable)

    // Getters/Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public PasswordResetOtp setEmail(String email) { this.email = email; return this; }

    public String getCode() { return code; }
    public PasswordResetOtp setCode(String code) { this.code = code; return this; }

    public String getPurpose() { return purpose; }
    public PasswordResetOtp setPurpose(String purpose) { this.purpose = purpose; return this; }

    public Instant getCreatedAt() { return createdAt; }
    public PasswordResetOtp setCreatedAt(Instant createdAt) { this.createdAt = createdAt; return this; }

    public Instant getExpiresAt() { return expiresAt; }
    public PasswordResetOtp setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }

    public int getAttempts() { return attempts; }
    public PasswordResetOtp setAttempts(int attempts) { this.attempts = attempts; return this; }

    public boolean isUsed() { return used; }
    public PasswordResetOtp setUsed(boolean used) { this.used = used; return this; }

    public String getRequestedBy() { return requestedBy; }
    public PasswordResetOtp setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; return this; }
}

package com.autobridge_api.auth.reset.dto;

public class ForgotPasswordRequest {
    private String email;
    private String purpose;     // optional: "SELF_RESET" (default) or "ADMIN_RESET"
    private String requestedBy; // optional admin email for audit

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
}

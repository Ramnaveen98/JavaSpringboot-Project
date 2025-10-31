package com.autobridge_api.auth.reset.dto;

public class VerifyOtpRequest {
    private String email;
    private String code;
    private String purpose; // optional

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}

package com.autobridge_api.auth.reset;

import com.autobridge_api.auth.reset.dto.ForgotPasswordRequest;
import com.autobridge_api.auth.reset.dto.ResetPasswordRequest;
import com.autobridge_api.auth.reset.dto.VerifyOtpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthPasswordController {

    private final PasswordResetService service;

    public AuthPasswordController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody ForgotPasswordRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        if (req.getPurpose() == null || req.getPurpose().isBlank()) {
            req.setPurpose("SELF_RESET");
        }
        service.forgot(req);
        return ResponseEntity.ok().body("OTP sent to email if it exists.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verify(@RequestBody VerifyOtpRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank() || req.getCode() == null || req.getCode().isBlank()) {
            return ResponseEntity.badRequest().body("Email and code are required.");
        }
        service.verify(req);
        return ResponseEntity.ok().body("OTP verified.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody ResetPasswordRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank() ||
                req.getCode() == null || req.getCode().isBlank() ||
                req.getNewPassword() == null || req.getNewPassword().isBlank() ||
                req.getConfirmPassword() == null || req.getConfirmPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Email, code, newPassword, confirmPassword are required.");
        }
        service.reset(req);
        return ResponseEntity.ok().body("Password updated successfully.");
    }
}

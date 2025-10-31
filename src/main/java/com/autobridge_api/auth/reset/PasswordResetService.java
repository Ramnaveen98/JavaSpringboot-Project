package com.autobridge_api.auth.reset;

import com.autobridge_api.auth.reset.dto.ForgotPasswordRequest;
import com.autobridge_api.auth.reset.dto.ResetPasswordRequest;
import com.autobridge_api.auth.reset.dto.VerifyOtpRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

    private static final Duration DEFAULT_EXPIRY = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private final PasswordResetOtpRepository repo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${autobridge.auth.user-table-name:users}")
    private String userTable;

    @Value("${autobridge.auth.user-email-column:email}")
    private String emailColumn;

    @Value("${autobridge.auth.user-password-column:password}")
    private String passwordColumn;

    @Value("${autobridge.mail.from-name:AutoBridge Support}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public PasswordResetService(
            PasswordResetOtpRepository repo,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate
    ) {
        this.repo = repo;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void forgot(ForgotPasswordRequest req) {
        final String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        final String purpose = (req.getPurpose() == null || req.getPurpose().isBlank())
                ? "SELF_RESET" : req.getPurpose().trim();

        // Invalidate any active OTP for this (email,purpose)
        repo.markActiveAsUsed(email, purpose);
        repo.deleteExpired(Instant.now());

        // Create a new OTP
        String code = generateOtp();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(DEFAULT_EXPIRY);

        PasswordResetOtp otp = new PasswordResetOtp()
                .setEmail(email)
                .setPurpose(purpose)
                .setCode(code)
                .setCreatedAt(now)
                .setExpiresAt(expiresAt)
                .setUsed(false)
                .setRequestedBy(req.getRequestedBy());

        repo.save(otp);

        // Email the OTP
        sendOtpEmail(email, code, DEFAULT_EXPIRY, "ADMIN_RESET".equals(purpose), req.getRequestedBy());
    }

    @Transactional
    public void verify(VerifyOtpRequest req) {
        final String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        final String purpose = (req.getPurpose() == null || req.getPurpose().isBlank())
                ? "SELF_RESET" : req.getPurpose().trim();

        PasswordResetOtp otp = repo.findByEmailAndPurposeAndUsedFalse(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("No active OTP found. Please request a new one."));

        if (Instant.now().isAfter(otp.getExpiresAt())) {
            repo.markActiveAsUsed(email, purpose);
            throw new IllegalArgumentException("OTP expired. Please request a new one.");
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            repo.markActiveAsUsed(email, purpose);
            throw new IllegalArgumentException("Too many attempts. Please request a new OTP.");
        }

        if (!Objects.equals(otp.getCode(), req.getCode())) {
            otp.setAttempts(otp.getAttempts() + 1);
            repo.save(otp);
            throw new IllegalArgumentException("Invalid OTP.");
        }
        // Do not mark used here; mark used during reset after successful password change.
    }

    @Transactional
    public void reset(ResetPasswordRequest req) {
        if (!Objects.equals(req.getNewPassword(), req.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        final String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        final String purpose = (req.getPurpose() == null || req.getPurpose().isBlank())
                ? "SELF_RESET" : req.getPurpose().trim();

        PasswordResetOtp otp = repo.findByEmailAndPurposeAndUsedFalse(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("No active OTP found. Please request a new one."));

        if (Instant.now().isAfter(otp.getExpiresAt())) {
            repo.markActiveAsUsed(email, purpose);
            throw new IllegalArgumentException("OTP expired. Please request a new one.");
        }
        if (!Objects.equals(otp.getCode(), req.getCode())) {
            otp.setAttempts(otp.getAttempts() + 1);
            repo.save(otp);
            throw new IllegalArgumentException("Invalid OTP.");
        }

        // Encode and update password in your users table via JdbcTemplate.
        String hashed = passwordEncoder.encode(req.getNewPassword());
        int updated = jdbcTemplate.update(
                "UPDATE " + userTable + " SET " + passwordColumn + " = ? WHERE " + emailColumn + " = ?",
                hashed, email
        );
        if (updated == 0) {
            throw new IllegalArgumentException("User not found for email: " + email);
        }

        // Consume this OTP
        otp.setUsed(true);
        repo.save(otp);

        // Optional: cleanup old expired rows to keep table tidy
        repo.deleteExpired(Instant.now());
    }

    private static String generateOtp() {
        // 6-digit numeric
        Random rnd = new Random();
        int n = 100000 + rnd.nextInt(900000);
        return String.valueOf(n);
    }

    private void sendOtpEmail(String to, String code, Duration expiresIn, boolean adminInitiated, String requestedBy) {
        String subject = adminInitiated ? "AutoBridge: Admin Password Reset OTP" : "AutoBridge: Password Reset OTP";
        String body = (adminInitiated
                ? "An administrator"
                : "You") + " requested a password reset for your AutoBridge account.\n\n" +
                "OTP Code: " + code + "\n" +
                "This code expires in " + expiresIn.toMinutes() + " minutes.\n\n" +
                (adminInitiated && requestedBy != null ? ("Requested by: " + requestedBy + "\n\n") : "") +
                "If you did not request this, please ignore this email.";

        SimpleMailMessage msg = new SimpleMailMessage();
        // from may be ignored by some providers; Spring will use spring.mail.username
        msg.setFrom(fromEmail == null || fromEmail.isBlank() ? "no-reply@autobridge.local" : fromEmail);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}

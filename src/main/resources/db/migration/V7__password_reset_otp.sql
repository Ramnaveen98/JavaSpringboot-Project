-- V7__password_reset_otp.sql
-- Creates the password_reset_otp table for Forgot/Reset Password (SELF_RESET)
-- and Admin Reset Password (ADMIN_RESET) flows.
-- Tested for MySQL 8.x / 9.x.

-- ==========================================
-- 1️⃣ Table Definition
-- ==========================================
CREATE TABLE IF NOT EXISTS password_reset_otp (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    code          VARCHAR(6)   NOT NULL,
    purpose       VARCHAR(32)  NOT NULL, -- 'SELF_RESET' | 'ADMIN_RESET'
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    TIMESTAMP    NOT NULL,
    attempts      INT          NOT NULL DEFAULT 0,
    used          BOOLEAN      NOT NULL DEFAULT FALSE,
    requested_by  VARCHAR(255),          -- admin email for admin resets (nullable)
    CONSTRAINT uq_pwdreset_active UNIQUE (email, purpose, used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- 2️⃣ Indexes for faster lookups
-- ==========================================
CREATE INDEX idx_pwdreset_email   ON password_reset_otp (email);
CREATE INDEX idx_pwdreset_expires ON password_reset_otp (expires_at);

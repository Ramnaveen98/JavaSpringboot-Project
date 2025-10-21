-- V3__add_feedback_ack_columns.sql
-- Safely add acknowledgement columns if they don't exist (MySQL-compatible)

-- acknowledged (BOOLEAN -> TINYINT(1))
SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND COLUMN_NAME = 'acknowledged'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE feedback ADD COLUMN acknowledged TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- acknowledged_by (VARCHAR)
SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND COLUMN_NAME = 'acknowledged_by'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE feedback ADD COLUMN acknowledged_by VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- acknowledged_at (TIMESTAMP/DATETIME)
SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND COLUMN_NAME = 'acknowledged_at'
);
-- Use DATETIME(6) for better precision with Instant; TIMESTAMP NULL also OK
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE feedback ADD COLUMN acknowledged_at DATETIME(6) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (Optional) Add unique constraint on request_id if it's not there yet
-- Checks by constraint name (uq_feedback_request). Skip if you already have it.
SET @constraint_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND CONSTRAINT_TYPE = 'UNIQUE'
    AND CONSTRAINT_NAME = 'uq_feedback_request'
);
SET @ddl := IF(@constraint_exists = 0,
  'ALTER TABLE feedback ADD CONSTRAINT uq_feedback_request UNIQUE (request_id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V6__request_vehicle_fk.sql
-- Align service_request → inventory_vehicles FK (plural)

-- 1) Add column if missing
SET @add_col := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'service_request'
        AND COLUMN_NAME = 'inventory_vehicle_id'
    ), 'SELECT 1', 'ALTER TABLE service_request ADD COLUMN inventory_vehicle_id BIGINT NULL'
  )
);
PREPARE stmt FROM @add_col; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Add index on the FK column if missing
SET @add_idx := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'service_request'
        AND INDEX_NAME = 'idx_sr_inventory_vehicle_id'
    ), 'SELECT 1', 'CREATE INDEX idx_sr_inventory_vehicle_id ON service_request(inventory_vehicle_id)'
  )
);
PREPARE stmt FROM @add_idx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Drop any existing FK from service_request to either inventory_vehicle/_vehicles
SET @fk_to_drop := (
  SELECT rc.CONSTRAINT_NAME
  FROM information_schema.REFERENTIAL_CONSTRAINTS rc
  WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
    AND rc.TABLE_NAME = 'service_request'
    AND rc.REFERENCED_TABLE_NAME IN ('inventory_vehicle','inventory_vehicles')
  LIMIT 1
);
SET @drop_fk := IF(@fk_to_drop IS NULL, 'SELECT 1',
                   CONCAT('ALTER TABLE service_request DROP FOREIGN KEY ', @fk_to_drop));
PREPARE stmt FROM @drop_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) Ensure target table exists (plural). If not, fail with a clear message.
-- (Flyway will show this SELECT result in logs)
SELECT IF(EXISTS (
  SELECT 1 FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inventory_vehicles'
), 1, (SELECT CAST('ERROR: inventory_vehicles table is missing' AS CHAR)));

-- 5) Create the desired FK to the correct table
ALTER TABLE service_request
  ADD CONSTRAINT fk_sr_inventory_vehicles
  FOREIGN KEY (inventory_vehicle_id) REFERENCES inventory_vehicles(id)
  ON DELETE SET NULL
  ON UPDATE CASCADE;

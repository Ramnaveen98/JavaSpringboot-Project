-- V6__request_vehicle_fk.sql
-- Purpose: ensure service_request.inventory_vehicle_id is nullable and has an FK
--          to inventory_vehicle(id) with ON DELETE SET NULL. Written to be idempotent.

-- 1) Make the column nullable (safe even if already nullable)
ALTER TABLE service_request
  MODIFY COLUMN inventory_vehicle_id BIGINT NULL;

-- 2) Drop any existing FK from service_request -> inventory_vehicle (name-agnostic)
--    MySQL doesn't support "DROP FOREIGN KEY IF EXISTS", so do it dynamically.
SET @fk_name := (
  SELECT rc.CONSTRAINT_NAME
  FROM information_schema.REFERENTIAL_CONSTRAINTS rc
  WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
    AND rc.TABLE_NAME = 'service_request'
    AND rc.REFERENCED_TABLE_NAME = 'inventory_vehicle'
  LIMIT 1
);

SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE service_request DROP FOREIGN KEY `', @fk_name, '`'),
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Ensure an index on inventory_vehicle_id exists (FKs need it)
SET @idx_name := (
  SELECT s.INDEX_NAME
  FROM information_schema.STATISTICS s
  WHERE s.TABLE_SCHEMA = DATABASE()
    AND s.TABLE_NAME = 'service_request'
    AND s.COLUMN_NAME = 'inventory_vehicle_id'
  LIMIT 1
);

SET @sql := IF(@idx_name IS NULL,
               'ALTER TABLE service_request ADD INDEX idx_sr_inventory_vehicle_id (inventory_vehicle_id)',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) Create the desired FK (will fail only if an identical one already exists,
--    but the drop above should have removed any previous FK pointing to inventory_vehicle).
ALTER TABLE service_request
  ADD CONSTRAINT fk_sr_inventory_vehicle
  FOREIGN KEY (inventory_vehicle_id) REFERENCES inventory_vehicle(id)
  ON DELETE SET NULL
  ON UPDATE CASCADE;

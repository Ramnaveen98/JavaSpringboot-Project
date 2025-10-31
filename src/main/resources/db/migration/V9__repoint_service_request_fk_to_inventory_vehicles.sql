-- V9__repoint_service_request_fk_to_inventory_vehicles.sql

-- Ensure the column exists & is nullable
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'service_request'
    AND COLUMN_NAME = 'inventory_vehicle_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE service_request ADD COLUMN inventory_vehicle_id BIGINT NULL',
  'ALTER TABLE service_request MODIFY COLUMN inventory_vehicle_id BIGINT NULL'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Clean orphans to avoid FK error
UPDATE service_request r
LEFT JOIN inventory_vehicles v ON v.id = r.inventory_vehicle_id
SET r.inventory_vehicle_id = NULL
WHERE r.inventory_vehicle_id IS NOT NULL AND v.id IS NULL;

-- Drop any pre-existing FK (whatever its name / singular or plural)
SET @fk_name := (
  SELECT rc.CONSTRAINT_NAME
  FROM information_schema.REFERENTIAL_CONSTRAINTS rc
  WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
    AND rc.TABLE_NAME = 'service_request'
    AND rc.REFERENCED_TABLE_NAME IN ('inventory_vehicles','inventory_vehicle')
  LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
  CONCAT('ALTER TABLE service_request DROP FOREIGN KEY `', @fk_name, '`'),
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Ensure index on FK column
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
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add correct FK to plural table
ALTER TABLE service_request
  ADD CONSTRAINT fk_sr_inventory_vehicle
  FOREIGN KEY (inventory_vehicle_id) REFERENCES inventory_vehicles(id)
  ON DELETE SET NULL ON UPDATE CASCADE;

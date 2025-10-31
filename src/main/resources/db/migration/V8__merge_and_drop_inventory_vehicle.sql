-- Merge legacy `inventory_vehicle` -> `inventory_vehicles`,
-- repoint FK from service_request, then drop legacy table.

START TRANSACTION;

-- 1) Ensure target table has legacy metadata columns (portable, no IF NOT EXISTS syntax needed)
-- image_url
SET @sql := (
  SELECT CASE WHEN COUNT(*)=0 THEN
    'ALTER TABLE `inventory_vehicles` ADD COLUMN `image_url` varchar(512) NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name='inventory_vehicles' AND column_name='image_url'
);
PREPARE s1 FROM @sql; EXECUTE s1; DEALLOCATE PREPARE s1;

-- description
SET @sql := (
  SELECT CASE WHEN COUNT(*)=0 THEN
    'ALTER TABLE `inventory_vehicles` ADD COLUMN `description` text NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name='inventory_vehicles' AND column_name='description'
);
PREPARE s2 FROM @sql; EXECUTE s2; DEALLOCATE PREPARE s2;

-- created_at
SET @sql := (
  SELECT CASE WHEN COUNT(*)=0 THEN
    'ALTER TABLE `inventory_vehicles` ADD COLUMN `created_at` datetime(6) NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name='inventory_vehicles' AND column_name='created_at'
);
PREPARE s3 FROM @sql; EXECUTE s3; DEALLOCATE PREPARE s3;

-- updated_at
SET @sql := (
  SELECT CASE WHEN COUNT(*)=0 THEN
    'ALTER TABLE `inventory_vehicles` ADD COLUMN `updated_at` datetime(6) NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name='inventory_vehicles' AND column_name='updated_at'
);
PREPARE s4 FROM @sql; EXECUTE s4; DEALLOCATE PREPARE s4;

-- 2) Update existing rows from legacy by VIN (if legacy table exists)
SET @sql := (
  SELECT CASE WHEN COUNT(*)>0 THEN
    'UPDATE `inventory_vehicles` v
       JOIN `inventory_vehicle` iv ON iv.vin = v.vin
       SET v.color       = COALESCE(v.color, iv.color),
           v.price       = COALESCE(v.price, iv.price),
           v.`status`    = COALESCE(v.`status`, iv.`status`),
           v.image_url   = COALESCE(v.image_url, iv.image_url),
           v.description = COALESCE(v.description, iv.description),
           v.created_at  = COALESCE(v.created_at, iv.created_at),
           v.updated_at  = COALESCE(v.updated_at, iv.updated_at)'
  ELSE 'SELECT 1' END
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'inventory_vehicle'
);
PREPARE s5 FROM @sql; EXECUTE s5; DEALLOCATE PREPARE s5;

-- 3) Insert VINs present only in legacy (satisfy NOT NULL columns with safe defaults)
SET @sql := (
  SELECT CASE WHEN COUNT(*)>0 THEN
    'INSERT INTO `inventory_vehicles`
       (vin, title,     brand,     model, color, `year`, price, `status`,
        image_url, description, created_at, updated_at)
     SELECT
       iv.vin, ''Imported'', ''Unknown'', NULL, iv.color, NULL, iv.price, iv.`status`,
       iv.image_url, iv.description, iv.created_at, iv.updated_at
     FROM `inventory_vehicle` iv
     LEFT JOIN `inventory_vehicles` v ON v.vin = iv.vin
     WHERE v.id IS NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'inventory_vehicle'
);
PREPARE s6 FROM @sql; EXECUTE s6; DEALLOCATE PREPARE s6;

-- 4) Re-point foreign key on service_request from legacy -> new table

-- 4a) Add a temporary column to hold the new IDs (if not already there)
SET @sql := (
  SELECT CASE WHEN COUNT(*)=0 THEN
    'ALTER TABLE `service_request` ADD COLUMN `inventory_vehicle_id_new` BIGINT NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='service_request' AND column_name='inventory_vehicle_id_new'
);
PREPARE s7 FROM @sql; EXECUTE s7; DEALLOCATE PREPARE s7;

-- 4b) Populate temp column by joining legacy->new via VIN
SET @sql := (
  SELECT CASE WHEN COUNT(*)>0 THEN
    'UPDATE `service_request` sr
       JOIN `inventory_vehicle` iv ON iv.id = sr.inventory_vehicle_id
       JOIN `inventory_vehicles` v2 ON v2.vin = iv.vin
       SET sr.inventory_vehicle_id_new = v2.id
     WHERE sr.inventory_vehicle_id IS NOT NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name='inventory_vehicle'
);
PREPARE s8 FROM @sql; EXECUTE s8; DEALLOCATE PREPARE s8;

-- 4c) Drop FK that references legacy table (name discovered dynamically)
SET @fk := (
  SELECT rc.CONSTRAINT_NAME
  FROM information_schema.REFERENTIAL_CONSTRAINTS rc
  WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
    AND rc.TABLE_NAME = 'service_request'
    AND rc.REFERENCED_TABLE_NAME = 'inventory_vehicle'
  LIMIT 1
);
SET @sql := IF(@fk IS NOT NULL,
  CONCAT('ALTER TABLE `service_request` DROP FOREIGN KEY `', @fk, '`'),
  'SELECT 1'
);
PREPARE s9 FROM @sql; EXECUTE s9; DEALLOCATE PREPARE s9;

-- 4d) Drop old FK index if it exists (optional; ignore if absent)
SET @idx := (
  SELECT INDEX_NAME
  FROM information_schema.statistics
  WHERE table_schema=DATABASE()
    AND table_name='service_request'
    AND column_name='inventory_vehicle_id'
    AND INDEX_NAME <> 'PRIMARY'
  LIMIT 1
);
SET @sql := IF(@idx IS NOT NULL,
  CONCAT('ALTER TABLE `service_request` DROP INDEX `', @idx, '`'),
  'SELECT 1'
);
PREPARE s10 FROM @sql; EXECUTE s10; DEALLOCATE PREPARE s10;

-- 4e) Replace old column with the new IDs
-- Drop old column if present
SET @has_old := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='service_request' AND column_name='inventory_vehicle_id'
);
SET @sql := IF(@has_old>0,
  'ALTER TABLE `service_request` DROP COLUMN `inventory_vehicle_id`',
  'SELECT 1'
);
PREPARE s11 FROM @sql; EXECUTE s11; DEALLOCATE PREPARE s11;

-- Rename temp -> inventory_vehicle_id
SET @has_new := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='service_request' AND column_name='inventory_vehicle_id_new'
);
SET @sql := IF(@has_new>0,
  'ALTER TABLE `service_request` CHANGE COLUMN `inventory_vehicle_id_new` `inventory_vehicle_id` BIGINT NULL',
  'SELECT 1'
);
PREPARE s12 FROM @sql; EXECUTE s12; DEALLOCATE PREPARE s12;

-- 4f) Recreate index and FK to the new table
SET @sql := 'ALTER TABLE `service_request` ADD INDEX `idx_sr_inventory_vehicle_id` (`inventory_vehicle_id`)';
PREPARE s13 FROM @sql; EXECUTE s13; DEALLOCATE PREPARE s13;

SET @fk2 := (
  SELECT rc.CONSTRAINT_NAME
  FROM information_schema.REFERENTIAL_CONSTRAINTS rc
  WHERE rc.CONSTRAINT_SCHEMA=DATABASE()
    AND rc.TABLE_NAME='service_request'
    AND rc.REFERENCED_TABLE_NAME='inventory_vehicles'
  LIMIT 1
);
SET @sql := IF(@fk2 IS NULL,
  'ALTER TABLE `service_request`
     ADD CONSTRAINT `fk_sr_inventory_vehicles`
     FOREIGN KEY (`inventory_vehicle_id`)
     REFERENCES `inventory_vehicles`(`id`)
     ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1'
);
PREPARE s14 FROM @sql; EXECUTE s14; DEALLOCATE PREPARE s14;

-- 5) Drop the legacy table (now safe)
SET @sql := (
  SELECT CASE WHEN COUNT(*)>0 THEN 'DROP TABLE `inventory_vehicle`' ELSE 'SELECT 1' END
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'inventory_vehicle'
);
PREPARE s15 FROM @sql; EXECUTE s15; DEALLOCATE PREPARE s15;

COMMIT;

-- Merge legacy `inventory_vehicle` -> `inventory_vehicles`,
-- repoint FK from service_request, then drop legacy table.
-- Script is idempotent across partial runs.

START TRANSACTION;

-- 1) Ensure target table has legacy metadata columns

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

-- 2) Update rows in target from legacy by VIN (only if legacy exists)
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

-- 3) Insert VINs only in legacy
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

-- 4) Re-point foreign key on service_request

-- 4a) Temp column to hold new IDs
SET @sql := (
  SELECT CASE WHEN COUNT(*)=0 THEN
    'ALTER TABLE `service_request` ADD COLUMN `inventory_vehicle_id_new` BIGINT NULL'
  ELSE 'SELECT 1' END
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='service_request' AND column_name='inventory_vehicle_id_new'
);
PREPARE s7 FROM @sql; EXECUTE s7; DEALLOCATE PREPARE s7;

-- 4b) Populate temp by joining legacy->new via VIN (only if legacy table exists)
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

-- 4c) Drop ANY FK on service_request.inventory_vehicle_id (legacy or new)
DROP PROCEDURE IF EXISTS drop_all_sr_fk_on_inv_id;
DELIMITER $$
CREATE PROCEDURE drop_all_sr_fk_on_inv_id()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE fkname VARCHAR(128);

  DECLARE cur CURSOR FOR
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'service_request'
      AND COLUMN_NAME = 'inventory_vehicle_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO fkname;
    IF done = 1 THEN LEAVE read_loop; END IF;

    SET @sql := CONCAT('ALTER TABLE `service_request` DROP FOREIGN KEY `', fkname, '`');
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL drop_all_sr_fk_on_inv_id();
DROP PROCEDURE IF EXISTS drop_all_sr_fk_on_inv_id;

-- 4d) (intentionally no index drop; keeping existing index is fine)

-- 4e) Swap columns safely and idempotently
-- If old column exists, drop it (FKs already removed)
SET @has_old := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='service_request' AND column_name='inventory_vehicle_id'
);
SET @sql := IF(@has_old>0,
  'ALTER TABLE `service_request` DROP COLUMN `inventory_vehicle_id`',
  'SELECT 1'
);
PREPARE s11 FROM @sql; EXECUTE s11; DEALLOCATE PREPARE s11;

-- Rename temp -> inventory_vehicle_id if temp exists
SET @has_new := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='service_request' AND column_name='inventory_vehicle_id_new'
);
SET @sql := IF(@has_new>0,
  'ALTER TABLE `service_request` CHANGE COLUMN `inventory_vehicle_id_new` `inventory_vehicle_id` BIGINT NULL',
  'SELECT 1'
);
PREPARE s12 FROM @sql; EXECUTE s12; DEALLOCATE PREPARE s12;

-- 4f) Ensure index exists
SET @has_idx := (
  SELECT 1
  FROM information_schema.statistics
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'service_request'
    AND INDEX_NAME = 'idx_sr_inventory_vehicle_id'
  LIMIT 1
);
SET @sql := IF(@has_idx IS NULL,
  'ALTER TABLE `service_request` ADD INDEX `idx_sr_inventory_vehicle_id` (`inventory_vehicle_id`)',
  'SELECT 1'
);
PREPARE s13 FROM @sql; EXECUTE s13; DEALLOCATE PREPARE s13;

-- 4g) (Re)create FK to inventory_vehicles if missing
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

-- 5) Drop legacy table at the end
SET @sql := 'DROP TABLE IF EXISTS `inventory_vehicle`';
PREPARE s15 FROM @sql; EXECUTE s15; DEALLOCATE PREPARE s15;

COMMIT;

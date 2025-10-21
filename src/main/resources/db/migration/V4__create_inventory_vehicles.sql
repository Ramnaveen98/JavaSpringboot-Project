-- Creates inventory_vehicles for MySQL 8+/9.x (compatible).
-- Matches com.autobridge_api.vehicles.InventoryVehicle (snake_case column names).

CREATE TABLE IF NOT EXISTS inventory_vehicles (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  vin           VARCHAR(32) UNIQUE,
  title         VARCHAR(255) NOT NULL,
  brand         VARCHAR(255) NOT NULL,
  model         VARCHAR(64),
  color         VARCHAR(64),
  year          INT,
  price         DECIMAL(14,2),
  status        VARCHAR(32),
  image_url     VARCHAR(2048),
  description   TEXT,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add indexes (no IF NOT EXISTS; table is new in this migration)
CREATE INDEX idx_inventory_vehicles_created_at ON inventory_vehicles (created_at);
CREATE INDEX idx_inventory_vehicles_status     ON inventory_vehicles (status);
CREATE INDEX idx_inventory_vehicles_brand      ON inventory_vehicles (brand);
CREATE INDEX idx_inventory_vehicles_price      ON inventory_vehicles (price);

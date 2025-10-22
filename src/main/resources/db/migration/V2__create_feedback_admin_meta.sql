-- admin-side flag for acknowledging feedback
CREATE TABLE IF NOT EXISTS feedback_admin_meta (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  feedback_id BIGINT NOT NULL UNIQUE,
  acknowledged BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

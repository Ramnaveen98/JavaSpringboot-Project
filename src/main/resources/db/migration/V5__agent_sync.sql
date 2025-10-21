ALTER TABLE agents
  ADD COLUMN user_id BIGINT NULL;

UPDATE agents a
JOIN users u ON LOWER(u.email) = LOWER(a.email)
SET a.user_id = u.id
WHERE a.user_id IS NULL;

ALTER TABLE agents
  ADD CONSTRAINT fk_agents_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

CREATE UNIQUE INDEX ux_agents_user_id ON agents(user_id);

CREATE UNIQUE INDEX ux_agents_email ON agents(email);

DROP VIEW IF EXISTS agents_directory_v;
CREATE VIEW agents_directory_v AS
SELECT
  a.id           AS agent_id,
  a.user_id      AS user_id,
  u.email        AS email,
  a.first_name   AS first_name,
  a.last_name    AS last_name,
  a.active       AS active,
  u.role         AS user_role
FROM agents a
JOIN users u ON u.id = a.user_id;

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_route_version_snapshot_identity; type=schema; riskLevel=high
-- Run the MES route snapshot READINESS command immediately before this migration.
-- The command strictly reparses and recomputes every stored snapshot across all tenants;
-- this migration performs the final no-gap database check and never repairs data.

DROP PROCEDURE IF EXISTS enforce_mes_route_version_snapshot_identity;
DELIMITER $$
CREATE PROCEDURE enforce_mes_route_version_snapshot_identity()
BEGIN
  DECLARE blocker_count BIGINT DEFAULT 0;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'route_snapshot_sha256'
  ) OR NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'route_snapshot_format_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'route snapshot identity nullable migration is missing';
  END IF;

  SELECT COUNT(*) INTO blocker_count
    FROM `mes_pro_route_version`
   WHERE `route_snapshot_sha256` IS NULL
      OR TRIM(`route_snapshot_sha256`) = ''
      OR `route_snapshot_sha256` NOT REGEXP '^[0-9a-f]{64}$'
      OR `route_snapshot_format_version` IS NULL
      OR `route_snapshot_format_version` <> 'MES_ROUTE_SNAPSHOT_CANONICAL_V1';

  IF blocker_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'route snapshot identity blockers must be zero before enforcement';
  END IF;

  ALTER TABLE `mes_pro_route_version`
    MODIFY COLUMN `route_snapshot_sha256` varchar(64) NOT NULL
      COMMENT 'MES_ROUTE_SNAPSHOT_CANONICAL_V1 SHA-256',
    MODIFY COLUMN `route_snapshot_format_version` varchar(32) NOT NULL
      COMMENT '路线快照canonical格式版本';
END$$
DELIMITER ;

CALL enforce_mes_route_version_snapshot_identity();

DROP PROCEDURE IF EXISTS enforce_mes_route_version_snapshot_identity;

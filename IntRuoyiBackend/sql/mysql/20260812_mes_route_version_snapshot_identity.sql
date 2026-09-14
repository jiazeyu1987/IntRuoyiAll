-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_route_version_lifecycle; type=schema; riskLevel=high
-- Phase 1 only: add nullable canonical hash columns. Backfill and NOT NULL enforcement
-- are separate gates and must not infer values from the current route projection.

DROP PROCEDURE IF EXISTS ensure_mes_route_version_snapshot_identity_schema;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_version_snapshot_identity_schema()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'route_snapshot_sha256'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `route_snapshot_sha256` varchar(64) NULL
        COMMENT 'MES_ROUTE_SNAPSHOT_CANONICAL_V1 SHA-256' AFTER `route_snapshot_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'route_snapshot_format_version'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `route_snapshot_format_version` varchar(32) NULL
        COMMENT '路线快照canonical格式版本' AFTER `route_snapshot_sha256`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_version_snapshot_identity_schema();

DROP PROCEDURE IF EXISTS ensure_mes_route_version_snapshot_identity_schema;

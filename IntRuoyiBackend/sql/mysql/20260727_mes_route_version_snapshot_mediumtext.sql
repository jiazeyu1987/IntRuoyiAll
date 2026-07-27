-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_route_version_lifecycle; type=schema; riskLevel=low
-- Purpose: Route candidate snapshots can exceed MySQL TEXT size once full BATCH form slots and formal batch record reports are preserved.

DROP PROCEDURE IF EXISTS ensure_mes_route_version_snapshot_mediumtext;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_version_snapshot_mediumtext()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_version is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'route_snapshot_json'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_version.route_snapshot_json is missing';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'route_snapshot_json'
       AND DATA_TYPE NOT IN ('mediumtext', 'longtext', 'json')
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      MODIFY COLUMN `route_snapshot_json` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL
      COMMENT '创建时冻结路线快照';
  END IF;
END $$
DELIMITER ;

CALL ensure_mes_route_version_snapshot_mediumtext();
DROP PROCEDURE IF EXISTS ensure_mes_route_version_snapshot_mediumtext;

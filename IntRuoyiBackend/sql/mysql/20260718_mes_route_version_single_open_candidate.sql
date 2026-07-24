-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_route_version_lifecycle,20260717_mes_route_version_approval_instance_id_string; type=schema; riskLevel=medium
-- Purpose: 同一租户同一工艺路线只允许一个未完成候选版本，范围为 DRAFT / PENDING_APPROVAL / READY_TO_PUBLISH。

DROP PROCEDURE IF EXISTS ensure_mes_route_version_single_open_candidate;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_version_single_open_candidate()
BEGIN
  IF EXISTS (
    SELECT 1
      FROM `mes_pro_route_version`
     WHERE `deleted` = b'0'
       AND `active` = b'0'
       AND `lifecycle_status` IN ('DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH')
     GROUP BY `tenant_id`, `route_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'duplicate open route candidate versions must be resolved before migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'open_candidate_route_id'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `open_candidate_route_id` bigint
        GENERATED ALWAYS AS (
          CASE
            WHEN `deleted` = b'0'
             AND `active` = b'0'
             AND `lifecycle_status` IN ('DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH')
            THEN `route_id`
            ELSE NULL
          END
        ) STORED
        COMMENT '未完成候选版本唯一约束路线标记' AFTER `active_unique_flag`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND INDEX_NAME = 'uk_mes_route_version_open_candidate'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD UNIQUE INDEX `uk_mes_route_version_open_candidate`
        (`tenant_id`, `open_candidate_route_id`) USING BTREE;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_version_single_open_candidate();

DROP PROCEDURE IF EXISTS ensure_mes_route_version_single_open_candidate;

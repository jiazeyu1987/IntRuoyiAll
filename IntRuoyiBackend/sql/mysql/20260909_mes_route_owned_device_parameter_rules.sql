-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260810_mes_process_pool_device_parameter_select_options; type=schema; riskLevel=medium
-- Purpose: device parameter rules are owned by route process versions. leader_user_id is kept only as an optional last-maintainer field for historical team-leader edits.
-- Recovery: the DDL is idempotent through information_schema guards; rerun is safe after checking migration ledger.
-- Rollback blocker: before reverting to NOT NULL, prove every active rule has a non-null leader_user_id and no route-owned editor is deployed.

DROP PROCEDURE IF EXISTS ensure_mes_route_owned_device_parameter_rules;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_owned_device_parameter_rules()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES device parameter rule table';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name = 'leader_user_id'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES device parameter rule leader_user_id column';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name = 'leader_user_id'
      AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_device_parameter_rule`
      MODIFY COLUMN `leader_user_id` bigint DEFAULT NULL COMMENT '最后维护人用户ID，设备参数规则归属工艺路线工序';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_owned_device_parameter_rules();

DROP PROCEDURE IF EXISTS ensure_mes_route_owned_device_parameter_rules;

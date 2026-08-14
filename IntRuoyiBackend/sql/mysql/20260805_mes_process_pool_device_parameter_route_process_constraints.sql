-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260731_mes_process_pool_team_leader_p1_runtime_config; type=schema; riskLevel=medium
-- P1: device parameter rules require formal route-process context and a target value.
-- Recovery: if preflight SIGNAL fails, the target table is unchanged; drop the helper procedure before retry.
-- Recovery: the next retry also starts with DROP PROCEDURE IF EXISTS, and the final ALTER TABLE is one atomic MySQL DDL statement.
-- Recovery: if the ALTER fails, retain the pre-migration backup and investigate the reported schema or data blocker.
-- Rollback blocker: before rollback, prove there are no duplicates under tenant_id + process_id + device_id + parameter_code + deleted.
-- Rollback before route-process-scoped writes: drop uk_mes_pp_device_parameter_route_process, restore uk_mes_pp_device_parameter_rule,
-- then make route_process_id/default_value nullable only if the prior application contract is also restored.

DROP PROCEDURE IF EXISTS preflight_mes_pp_device_parameter_route_process_constraints;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_device_parameter_route_process_constraints()
BEGIN
  DECLARE v_null_rule_count bigint DEFAULT 0;
  DECLARE v_duplicate_rule_count bigint DEFAULT 0;
  DECLARE v_legacy_index_columns varchar(512) DEFAULT NULL;
  DECLARE v_legacy_index_non_unique int DEFAULT NULL;
  DECLARE v_new_index_count int DEFAULT 0;

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
      AND column_name = 'route_process_id'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES parameter rule route_process_id column';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name = 'default_value'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES parameter rule default_value column';
  END IF;

  SELECT COUNT(*) INTO v_null_rule_count
  FROM `mes_pro_process_pool_device_parameter_rule`
  WHERE `route_process_id` IS NULL
     OR `default_value` IS NULL;

  IF v_null_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES parameter rule NULL route_process_id/default_value; complete formal data governance first';
  END IF;

  SELECT COUNT(*) INTO v_duplicate_rule_count
  FROM (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule`
    GROUP BY `tenant_id`, `route_process_id`, `device_id`, `parameter_code`, `deleted`
    HAVING COUNT(*) > 1
  ) AS duplicate_route_process_rules;

  IF v_duplicate_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate MES route-process device parameter rules block unique constraint';
  END IF;

  SELECT
    GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ','),
    MIN(non_unique)
  INTO v_legacy_index_columns, v_legacy_index_non_unique
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_process_pool_device_parameter_rule'
    AND index_name = 'uk_mes_pp_device_parameter_rule';

  IF v_legacy_index_columns IS NULL
     OR v_legacy_index_columns <> 'tenant_id,process_id,device_id,parameter_code,deleted'
     OR v_legacy_index_non_unique <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Legacy MES device parameter unique index is missing or unexpected';
  END IF;

  SELECT COUNT(*) INTO v_new_index_count
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_process_pool_device_parameter_rule'
    AND index_name = 'uk_mes_pp_device_parameter_route_process';

  IF v_new_index_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route-process parameter unique index already exists outside migration history';
  END IF;
END$$
DELIMITER ;

CALL preflight_mes_pp_device_parameter_route_process_constraints();

DROP PROCEDURE IF EXISTS preflight_mes_pp_device_parameter_route_process_constraints;

ALTER TABLE `mes_pro_process_pool_device_parameter_rule`
  MODIFY COLUMN `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
  MODIFY COLUMN `default_value` decimal(24,6) NOT NULL COMMENT '目标值',
  DROP INDEX `uk_mes_pp_device_parameter_rule`,
  ADD UNIQUE KEY `uk_mes_pp_device_parameter_route_process`
    (`tenant_id`, `route_process_id`, `device_id`, `parameter_code`, `deleted`);

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_process_pool_device_parameter_route_process_constraints; type=schema; riskLevel=medium
-- Device parameter rules preserve the exact source standard and allow text-only or targetless range standards.
-- Recovery: if preflight SIGNAL fails, no schema change occurs. Govern every existing row with an exact standard before retrying.
-- Recovery: the ALTER TABLE is one atomic MySQL DDL statement; retain the pre-migration backup if the DDL fails.
-- Rollback blocker: rollback is allowed only after proving every rule has numeric lower/upper/target values.
-- Rollback: drop standard_text and restore lower_limit/upper_limit/default_value NOT NULL with the prior application contract.

DROP PROCEDURE IF EXISTS preflight_mes_pp_device_parameter_standard_text;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_device_parameter_standard_text()
BEGIN
  DECLARE v_existing_rule_count bigint DEFAULT 0;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES device parameter rule table';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name = 'standard_text'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES parameter rule standard_text already exists outside migration history';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name IN ('lower_limit', 'upper_limit', 'default_value', 'value_type')
    GROUP BY table_name
    HAVING COUNT(*) = 4
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES parameter rule numeric or value_type columns';
  END IF;

  SELECT COUNT(*) INTO v_existing_rule_count
  FROM `mes_pro_process_pool_device_parameter_rule`;

  IF v_existing_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing MES device parameter rules require explicit standard_text governance';
  END IF;
END$$
DELIMITER ;

CALL preflight_mes_pp_device_parameter_standard_text();

DROP PROCEDURE IF EXISTS preflight_mes_pp_device_parameter_standard_text;

ALTER TABLE `mes_pro_process_pool_device_parameter_rule`
  MODIFY COLUMN `lower_limit` decimal(24,6) DEFAULT NULL COMMENT '下限；文本标准为空',
  MODIFY COLUMN `upper_limit` decimal(24,6) DEFAULT NULL COMMENT '上限；文本标准为空',
  MODIFY COLUMN `default_value` decimal(24,6) DEFAULT NULL COMMENT '目标值；范围标准和文本标准可为空',
  ADD COLUMN `standard_text` varchar(1000) NOT NULL COMMENT '参数标准原文' AFTER `value_type`;

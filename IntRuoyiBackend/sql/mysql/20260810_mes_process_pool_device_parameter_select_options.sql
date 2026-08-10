-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260807_mes_process_pool_device_parameter_standard_text; type=schema; riskLevel=medium
-- Device parameter rules support select options, text defaults and decimal scale for typed frontline parameter input.
-- Recovery: if preflight SIGNAL fails, no schema change occurs. Apply the dependency migration first, then retry.
-- Recovery: the ALTER TABLE is one atomic MySQL DDL statement; retain the pre-migration backup if the DDL fails.
-- Rollback blocker: rollback is allowed only after proving no SELECT rule depends on option_values_json/default_text/decimal_scale.
-- Rollback: drop chk_mes_pp_device_parameter_option_values_json, option_values_json, default_text and decimal_scale.

DROP PROCEDURE IF EXISTS preflight_mes_pp_device_parameter_select_options;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_device_parameter_select_options()
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
      AND column_name = 'standard_text'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing dependency column standard_text';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name IN ('option_values_json', 'default_text', 'decimal_scale')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES parameter select option columns already exist outside migration history';
  END IF;
END$$
DELIMITER ;

CALL preflight_mes_pp_device_parameter_select_options();

DROP PROCEDURE IF EXISTS preflight_mes_pp_device_parameter_select_options;

ALTER TABLE `mes_pro_process_pool_device_parameter_rule`
  ADD COLUMN `option_values_json` json DEFAULT NULL COMMENT '下拉选项JSON数组' AFTER `standard_text`,
  ADD COLUMN `default_text` varchar(128) DEFAULT NULL COMMENT '文本或下拉默认值' AFTER `option_values_json`,
  ADD COLUMN `decimal_scale` int DEFAULT NULL COMMENT '小数位数' AFTER `default_text`,
  ADD CONSTRAINT `chk_mes_pp_device_parameter_option_values_json`
    CHECK (JSON_TYPE(`option_values_json`) = 'ARRAY');

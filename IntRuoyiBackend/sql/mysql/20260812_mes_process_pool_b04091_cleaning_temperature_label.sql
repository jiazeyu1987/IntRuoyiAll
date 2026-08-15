-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_process_pool_cleaning_process_parameter_data; type=data; riskLevel=low
-- Rename only the B04091 cleaning-process room-temperature label to the business wording.
-- Recovery: reverse the same scoped update to parameter_name = '室温' and standard_text = REPLACE(standard_text, '清洗温度', '室温') if the business wording is withdrawn.

DROP PROCEDURE IF EXISTS preflight_mes_pp_b04091_cleaning_temperature_label;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_b04091_cleaning_temperature_label()
BEGIN
  IF (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN (
        'mes_pro_process_pool_device_parameter_rule',
        'mes_pro_process_pool_team_device',
        'mes_pro_process'
      )
  ) < 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES B04091 cleaning temperature dependency table';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND (
        (table_name = 'mes_pro_process_pool_device_parameter_rule'
          AND column_name IN ('parameter_code', 'parameter_name', 'standard_text'))
        OR (table_name = 'mes_pro_process_pool_team_device' AND column_name = 'device_code')
        OR (table_name = 'mes_pro_process' AND column_name = 'name')
      )
  ) < 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing B04091 cleaning temperature dependency column';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` rule
    JOIN `mes_pro_process_pool_team_device` device
      ON device.`id` = rule.`device_id`
     AND device.`tenant_id` = rule.`tenant_id`
     AND device.`deleted` = b'0'
    JOIN `mes_pro_process` process
      ON process.`id` = rule.`process_id`
     AND process.`tenant_id` = rule.`tenant_id`
     AND process.`deleted` = b'0'
    WHERE rule.`deleted` = b'0'
      AND process.`name` = '清洗工序'
      AND device.`device_code` = 'B04091'
      AND rule.`parameter_code` = 'CLEANING_ROOM_TEMPERATURE'
      AND rule.`parameter_name` IN ('室温', '清洗温度')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'No active B04091 cleaning temperature parameter rule found';
  END IF;
END$$
DELIMITER ;

CALL preflight_mes_pp_b04091_cleaning_temperature_label();

DROP PROCEDURE IF EXISTS preflight_mes_pp_b04091_cleaning_temperature_label;

START TRANSACTION;

UPDATE `mes_pro_process_pool_device_parameter_rule` rule
JOIN `mes_pro_process_pool_team_device` device
  ON device.`id` = rule.`device_id`
 AND device.`tenant_id` = rule.`tenant_id`
 AND device.`deleted` = b'0'
JOIN `mes_pro_process` process
  ON process.`id` = rule.`process_id`
 AND process.`tenant_id` = rule.`tenant_id`
 AND process.`deleted` = b'0'
SET
  rule.`parameter_code` = 'CLEANING_ROOM_TEMPERATURE',
  rule.`parameter_name` = '清洗温度',
  rule.`standard_text` = REPLACE(rule.`standard_text`, '室温', '清洗温度'),
  rule.`updater` = '20260812_b04091_cleaning_temperature_label',
  rule.`update_time` = NOW()
WHERE rule.`deleted` = b'0'
  AND process.`name` = '清洗工序'
  AND device.`device_code` = 'B04091'
  AND rule.`parameter_code` = 'CLEANING_ROOM_TEMPERATURE'
  AND rule.`parameter_name` IN ('室温', '清洗温度');

COMMIT;

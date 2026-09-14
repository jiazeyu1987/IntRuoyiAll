-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260810_mes_process_pool_device_parameter_select_options; type=data; riskLevel=medium
-- Normalize existing rough/fine wash ultrasonic cleaner parameter rules to the fixed typed cleaning-parameter contract.
-- Recovery: if preflight SIGNAL fails, no data update occurs. Resolve the duplicate or missing dependency first, then retry.
-- Recovery: retain the pre-migration database backup; rollback requires restoring the affected fixed cleaning-parameter rows.
-- Rollback blocker: old ad-hoc parameter codes/text standards are normalized to formal ROUGH_WASH and FINE_WASH identities.

DROP PROCEDURE IF EXISTS preflight_mes_pp_cleaning_wash_parameter_data;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_cleaning_wash_parameter_data()
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
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES process table';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_team_device'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES team device table';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name IN ('option_values_json', 'default_text', 'decimal_scale', 'standard_text')
  ) < 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing dependency columns for typed cleaning parameters';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT
        candidate.tenant_id,
        candidate.route_process_id,
        candidate.device_id,
        candidate.target_parameter_code,
        COUNT(*) AS target_count
      FROM (
        SELECT
          rule.id,
          rule.tenant_id,
          rule.route_process_id,
          rule.device_id,
          CASE
            WHEN process.`name` LIKE '%精洗%' AND (
              rule.`parameter_code` IN ('FINE_WASH_MEDIUM', 'ROUGH_WASH_MEDIUM')
              OR rule.`parameter_name` = '清洗介质'
            ) THEN 'FINE_WASH_MEDIUM'
            WHEN process.`name` LIKE '%粗洗%' AND (
              rule.`parameter_code` IN ('FINE_WASH_MEDIUM', 'ROUGH_WASH_MEDIUM')
              OR rule.`parameter_name` = '清洗介质'
            ) THEN 'ROUGH_WASH_MEDIUM'
            WHEN process.`name` LIKE '%精洗%' AND (
              rule.`parameter_code` IN ('FINE_WASH_ROOM_TEMPERATURE', 'ROUGH_WASH_ROOM_TEMPERATURE')
              OR rule.`parameter_name` IN ('室温', '清洗温度')
              OR rule.`standard_text` = '室温'
            ) THEN 'FINE_WASH_ROOM_TEMPERATURE'
            WHEN process.`name` LIKE '%粗洗%' AND (
              rule.`parameter_code` IN ('FINE_WASH_ROOM_TEMPERATURE', 'ROUGH_WASH_ROOM_TEMPERATURE')
              OR rule.`parameter_name` IN ('室温', '清洗温度')
              OR rule.`standard_text` = '室温'
            ) THEN 'ROUGH_WASH_ROOM_TEMPERATURE'
            ELSE rule.`parameter_code`
          END AS target_parameter_code
        FROM `mes_pro_process_pool_device_parameter_rule` rule
        JOIN `mes_pro_process` process
          ON process.`id` = rule.`process_id`
         AND process.`tenant_id` = rule.`tenant_id`
         AND process.`deleted` = b'0'
        JOIN `mes_pro_process_pool_team_device` device
          ON device.`id` = rule.`device_id`
         AND device.`tenant_id` = rule.`tenant_id`
         AND device.`deleted` = b'0'
        WHERE rule.`deleted` = b'0'
          AND rule.`route_process_id` IS NOT NULL
          AND (process.`name` LIKE '%精洗%' OR process.`name` LIKE '%粗洗%')
          AND (device.`device_name` LIKE '%超声波清洗机%' OR device.`device_code` LIKE '%超声波清洗机%')
          AND (
            rule.`parameter_code` IN (
              'FINE_WASH_MEDIUM',
              'ROUGH_WASH_MEDIUM',
              'FINE_WASH_ROOM_TEMPERATURE',
              'ROUGH_WASH_ROOM_TEMPERATURE'
            )
            OR rule.`parameter_name` IN ('清洗介质', '室温', '清洗温度')
            OR rule.`standard_text` = '室温'
          )
      ) candidate
      GROUP BY candidate.tenant_id, candidate.route_process_id, candidate.device_id,
               candidate.target_parameter_code
      HAVING target_count > 1
    ) duplicate_target
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate cleaning wash parameter rules would collide after normalization';
  END IF;
END$$
DELIMITER ;

CALL preflight_mes_pp_cleaning_wash_parameter_data();

DROP PROCEDURE IF EXISTS preflight_mes_pp_cleaning_wash_parameter_data;

START TRANSACTION;

UPDATE `mes_pro_process_pool_device_parameter_rule` rule
JOIN `mes_pro_process` process
  ON process.`id` = rule.`process_id`
 AND process.`tenant_id` = rule.`tenant_id`
 AND process.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` device
  ON device.`id` = rule.`device_id`
 AND device.`tenant_id` = rule.`tenant_id`
 AND device.`deleted` = b'0'
SET
  rule.`parameter_code` = CASE
    WHEN process.`name` LIKE '%精洗%' THEN 'FINE_WASH_MEDIUM'
    ELSE 'ROUGH_WASH_MEDIUM'
  END,
  rule.`parameter_name` = '清洗介质',
  rule.`unit` = NULL,
  rule.`lower_limit` = NULL,
  rule.`upper_limit` = NULL,
  rule.`default_value` = NULL,
  rule.`value_type` = 'SELECT',
  rule.`standard_text` = CASE
    WHEN process.`name` LIKE '%精洗%' THEN '清洗介质可选自来水或纯化水，默认纯化水'
    ELSE '清洗介质可选自来水或纯化水，默认自来水'
  END,
  rule.`option_values_json` = JSON_ARRAY('自来水', '纯化水'),
  rule.`default_text` = CASE
    WHEN process.`name` LIKE '%精洗%' THEN '纯化水'
    ELSE '自来水'
  END,
  rule.`decimal_scale` = NULL,
  rule.`updater` = '20260811_cleaning_wash_parameter_data',
  rule.`update_time` = NOW()
WHERE rule.`deleted` = b'0'
  AND rule.`route_process_id` IS NOT NULL
  AND (process.`name` LIKE '%精洗%' OR process.`name` LIKE '%粗洗%')
  AND (device.`device_name` LIKE '%超声波清洗机%' OR device.`device_code` LIKE '%超声波清洗机%')
  AND (
    rule.`parameter_code` IN ('FINE_WASH_MEDIUM', 'ROUGH_WASH_MEDIUM')
    OR rule.`parameter_name` = '清洗介质'
  );

UPDATE `mes_pro_process_pool_device_parameter_rule` rule
JOIN `mes_pro_process` process
  ON process.`id` = rule.`process_id`
 AND process.`tenant_id` = rule.`tenant_id`
 AND process.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` device
  ON device.`id` = rule.`device_id`
 AND device.`tenant_id` = rule.`tenant_id`
 AND device.`deleted` = b'0'
SET
  rule.`parameter_code` = CASE
    WHEN process.`name` LIKE '%精洗%' THEN 'FINE_WASH_ROOM_TEMPERATURE'
    ELSE 'ROUGH_WASH_ROOM_TEMPERATURE'
  END,
  rule.`parameter_name` = '室温',
  rule.`unit` = '℃',
  rule.`lower_limit` = 20,
  rule.`upper_limit` = 30,
  rule.`default_value` = 26,
  rule.`value_type` = 'DECIMAL',
  rule.`standard_text` = '室温 20.0-30.0℃，默认 26.0℃',
  rule.`option_values_json` = NULL,
  rule.`default_text` = NULL,
  rule.`decimal_scale` = 1,
  rule.`updater` = '20260811_cleaning_wash_parameter_data',
  rule.`update_time` = NOW()
WHERE rule.`deleted` = b'0'
  AND rule.`route_process_id` IS NOT NULL
  AND (process.`name` LIKE '%精洗%' OR process.`name` LIKE '%粗洗%')
  AND (device.`device_name` LIKE '%超声波清洗机%' OR device.`device_code` LIKE '%超声波清洗机%')
  AND (
    rule.`parameter_code` IN ('FINE_WASH_ROOM_TEMPERATURE', 'ROUGH_WASH_ROOM_TEMPERATURE')
    OR rule.`parameter_name` IN ('室温', '清洗温度')
    OR rule.`standard_text` = '室温'
  );

COMMIT;

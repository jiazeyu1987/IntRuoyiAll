-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_process_pool_cleaning_wash_parameter_data; type=data; riskLevel=medium
-- Normalize active cleaning-process ultrasonic cleaner rules to the formal CLEANING parameter contract.
-- Recovery: preflight failures occur before DML; resolve missing or duplicate formal rules, then retry.
-- Recovery: retain the pre-migration row snapshot; rollback restores only the frozen rule ids.

DROP PROCEDURE IF EXISTS preflight_mes_pp_cleaning_process_parameter_data;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_cleaning_process_parameter_data()
BEGIN
  IF (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN (
        'mes_pro_process_pool_device_parameter_rule',
        'mes_pro_process_pool_team_device',
        'mes_pro_process',
        'mes_pro_route_process'
      )
  ) < 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES cleaning process parameter dependency table';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name IN ('option_values_json', 'default_text', 'decimal_scale', 'standard_text')
  ) < 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing typed device parameter rule columns';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM mes_pro_process_pool_device_parameter_rule rule
    JOIN mes_pro_route_process route_process
      ON route_process.id = rule.route_process_id
     AND route_process.tenant_id = rule.tenant_id
     AND route_process.process_id = rule.process_id
     AND route_process.deleted = b'0'
    JOIN mes_pro_process process
      ON process.id = rule.process_id
     AND process.tenant_id = rule.tenant_id
     AND process.deleted = b'0'
    JOIN mes_pro_process_pool_team_device device
      ON device.id = rule.device_id
     AND device.tenant_id = rule.tenant_id
     AND device.deleted = b'0'
    WHERE rule.deleted = b'0'
      AND process.`name` = '清洗工序'
      AND device.device_name LIKE '%超声波清洗机%'
      AND (
        rule.parameter_code IN (
          'CLEANING_COUNT',
          'CLEANING_MEDIUM',
          'CLEANING_POWER',
          'CLEANING_ROOM_TEMPERATURE',
          'CLEANING_TIME'
        )
        OR rule.parameter_name IN ('清洗次数', '清洗介质', '清洗功率', '室温', '清洗温度', '清洗时间')
        OR rule.standard_text = '室温'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'No active cleaning process ultrasonic cleaner parameter rules found';
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
          rule.tenant_id,
          rule.route_process_id,
          rule.device_id,
          CASE
            WHEN rule.parameter_code = 'CLEANING_COUNT'
              OR rule.parameter_name = '清洗次数' THEN 'CLEANING_COUNT'
            WHEN rule.parameter_code = 'CLEANING_MEDIUM'
              OR rule.parameter_name = '清洗介质' THEN 'CLEANING_MEDIUM'
            WHEN rule.parameter_code = 'CLEANING_POWER'
              OR rule.parameter_name = '清洗功率' THEN 'CLEANING_POWER'
            WHEN rule.parameter_code = 'CLEANING_ROOM_TEMPERATURE'
              OR rule.parameter_name IN ('室温', '清洗温度')
              OR rule.standard_text = '室温' THEN 'CLEANING_ROOM_TEMPERATURE'
            WHEN rule.parameter_code = 'CLEANING_TIME'
              OR rule.parameter_name = '清洗时间' THEN 'CLEANING_TIME'
            ELSE NULL
          END AS target_parameter_code
        FROM mes_pro_process_pool_device_parameter_rule rule
        JOIN mes_pro_route_process route_process
          ON route_process.id = rule.route_process_id
         AND route_process.tenant_id = rule.tenant_id
         AND route_process.process_id = rule.process_id
         AND route_process.deleted = b'0'
        JOIN mes_pro_process process
          ON process.id = rule.process_id
         AND process.tenant_id = rule.tenant_id
         AND process.deleted = b'0'
        JOIN mes_pro_process_pool_team_device device
          ON device.id = rule.device_id
         AND device.tenant_id = rule.tenant_id
         AND device.deleted = b'0'
        WHERE rule.deleted = b'0'
          AND process.`name` = '清洗工序'
          AND device.device_name LIKE '%超声波清洗机%'
          AND (
            rule.parameter_code IN (
              'CLEANING_COUNT',
              'CLEANING_MEDIUM',
              'CLEANING_POWER',
              'CLEANING_ROOM_TEMPERATURE',
              'CLEANING_TIME'
            )
            OR rule.parameter_name IN ('清洗次数', '清洗介质', '清洗功率', '室温', '清洗温度', '清洗时间')
            OR rule.standard_text = '室温'
          )
      ) candidate
      WHERE candidate.target_parameter_code IS NOT NULL
      GROUP BY candidate.tenant_id, candidate.route_process_id, candidate.device_id,
               candidate.target_parameter_code
      HAVING target_count > 1
    ) duplicate_target
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate cleaning process parameter rules would collide after normalization';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT
        rule.tenant_id,
        rule.route_process_id,
        rule.device_id,
        COUNT(DISTINCT CASE
          WHEN rule.parameter_code = 'CLEANING_COUNT'
            OR rule.parameter_name = '清洗次数' THEN 'CLEANING_COUNT'
          WHEN rule.parameter_code = 'CLEANING_MEDIUM'
            OR rule.parameter_name = '清洗介质' THEN 'CLEANING_MEDIUM'
          WHEN rule.parameter_code = 'CLEANING_POWER'
            OR rule.parameter_name = '清洗功率' THEN 'CLEANING_POWER'
          WHEN rule.parameter_code = 'CLEANING_ROOM_TEMPERATURE'
            OR rule.parameter_name IN ('室温', '清洗温度')
            OR rule.standard_text = '室温' THEN 'CLEANING_ROOM_TEMPERATURE'
          WHEN rule.parameter_code = 'CLEANING_TIME'
            OR rule.parameter_name = '清洗时间' THEN 'CLEANING_TIME'
          ELSE NULL
        END) AS formal_parameter_count
      FROM mes_pro_process_pool_device_parameter_rule rule
      JOIN mes_pro_route_process route_process
        ON route_process.id = rule.route_process_id
       AND route_process.tenant_id = rule.tenant_id
       AND route_process.process_id = rule.process_id
       AND route_process.deleted = b'0'
      JOIN mes_pro_process process
        ON process.id = rule.process_id
       AND process.tenant_id = rule.tenant_id
       AND process.deleted = b'0'
      JOIN mes_pro_process_pool_team_device device
        ON device.id = rule.device_id
       AND device.tenant_id = rule.tenant_id
       AND device.deleted = b'0'
      WHERE rule.deleted = b'0'
        AND process.`name` = '清洗工序'
        AND device.device_name LIKE '%超声波清洗机%'
        AND (
          rule.parameter_code IN (
            'CLEANING_COUNT',
            'CLEANING_MEDIUM',
            'CLEANING_POWER',
            'CLEANING_ROOM_TEMPERATURE',
            'CLEANING_TIME'
          )
          OR rule.parameter_name IN ('清洗次数', '清洗介质', '清洗功率', '室温', '清洗温度', '清洗时间')
          OR rule.standard_text = '室温'
        )
      GROUP BY rule.tenant_id, rule.route_process_id, rule.device_id
      HAVING formal_parameter_count <> 5
    ) incomplete_target
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cleaning process ultrasonic cleaner must have exactly five formal parameter rules';
  END IF;
END$$
DELIMITER ;

CALL preflight_mes_pp_cleaning_process_parameter_data();

DROP PROCEDURE IF EXISTS preflight_mes_pp_cleaning_process_parameter_data;

START TRANSACTION;

UPDATE mes_pro_process_pool_device_parameter_rule rule
JOIN mes_pro_route_process route_process
  ON route_process.id = rule.route_process_id
 AND route_process.tenant_id = rule.tenant_id
 AND route_process.process_id = rule.process_id
 AND route_process.deleted = b'0'
JOIN mes_pro_process process
  ON process.id = rule.process_id
 AND process.tenant_id = rule.tenant_id
 AND process.deleted = b'0'
JOIN mes_pro_process_pool_team_device device
  ON device.id = rule.device_id
 AND device.tenant_id = rule.tenant_id
 AND device.deleted = b'0'
SET
  rule.parameter_code = 'CLEANING_COUNT',
  rule.parameter_name = '清洗次数',
  rule.updater = '20260811_cleaning_process_parameter_data',
  rule.update_time = NOW()
WHERE rule.deleted = b'0'
  AND process.`name` = '清洗工序'
  AND device.device_name LIKE '%超声波清洗机%'
  AND (rule.parameter_code = 'CLEANING_COUNT' OR rule.parameter_name = '清洗次数');

UPDATE mes_pro_process_pool_device_parameter_rule rule
JOIN mes_pro_route_process route_process
  ON route_process.id = rule.route_process_id
 AND route_process.tenant_id = rule.tenant_id
 AND route_process.process_id = rule.process_id
 AND route_process.deleted = b'0'
JOIN mes_pro_process process
  ON process.id = rule.process_id
 AND process.tenant_id = rule.tenant_id
 AND process.deleted = b'0'
JOIN mes_pro_process_pool_team_device device
  ON device.id = rule.device_id
 AND device.tenant_id = rule.tenant_id
 AND device.deleted = b'0'
SET
  rule.parameter_code = 'CLEANING_MEDIUM',
  rule.parameter_name = '清洗介质',
  rule.unit = NULL,
  rule.lower_limit = NULL,
  rule.upper_limit = NULL,
  rule.default_value = NULL,
  rule.value_type = 'SELECT',
  rule.standard_text = '清洗介质可选纯化水或自来水，默认纯化水',
  rule.option_values_json = JSON_ARRAY('纯化水', '自来水'),
  rule.default_text = '纯化水',
  rule.decimal_scale = NULL,
  rule.updater = '20260811_cleaning_process_parameter_data',
  rule.update_time = NOW()
WHERE rule.deleted = b'0'
  AND process.`name` = '清洗工序'
  AND device.device_name LIKE '%超声波清洗机%'
  AND (rule.parameter_code = 'CLEANING_MEDIUM' OR rule.parameter_name = '清洗介质');

UPDATE mes_pro_process_pool_device_parameter_rule rule
JOIN mes_pro_route_process route_process
  ON route_process.id = rule.route_process_id
 AND route_process.tenant_id = rule.tenant_id
 AND route_process.process_id = rule.process_id
 AND route_process.deleted = b'0'
JOIN mes_pro_process process
  ON process.id = rule.process_id
 AND process.tenant_id = rule.tenant_id
 AND process.deleted = b'0'
JOIN mes_pro_process_pool_team_device device
  ON device.id = rule.device_id
 AND device.tenant_id = rule.tenant_id
 AND device.deleted = b'0'
SET
  rule.parameter_code = 'CLEANING_POWER',
  rule.parameter_name = '清洗功率',
  rule.updater = '20260811_cleaning_process_parameter_data',
  rule.update_time = NOW()
WHERE rule.deleted = b'0'
  AND process.`name` = '清洗工序'
  AND device.device_name LIKE '%超声波清洗机%'
  AND (rule.parameter_code = 'CLEANING_POWER' OR rule.parameter_name = '清洗功率');

UPDATE mes_pro_process_pool_device_parameter_rule rule
JOIN mes_pro_route_process route_process
  ON route_process.id = rule.route_process_id
 AND route_process.tenant_id = rule.tenant_id
 AND route_process.process_id = rule.process_id
 AND route_process.deleted = b'0'
JOIN mes_pro_process process
  ON process.id = rule.process_id
 AND process.tenant_id = rule.tenant_id
 AND process.deleted = b'0'
JOIN mes_pro_process_pool_team_device device
  ON device.id = rule.device_id
 AND device.tenant_id = rule.tenant_id
 AND device.deleted = b'0'
SET
  rule.parameter_code = 'CLEANING_ROOM_TEMPERATURE',
  rule.parameter_name = '室温',
  rule.unit = '℃',
  rule.lower_limit = 20,
  rule.default_value = 26,
  rule.upper_limit = 30,
  rule.value_type = 'DECIMAL',
  rule.standard_text = '室温 20.0-30.0℃，默认 26.0℃',
  rule.option_values_json = NULL,
  rule.default_text = NULL,
  rule.decimal_scale = 1,
  rule.updater = '20260811_cleaning_process_parameter_data',
  rule.update_time = NOW()
WHERE rule.deleted = b'0'
  AND process.`name` = '清洗工序'
  AND device.device_name LIKE '%超声波清洗机%'
  AND (
    rule.parameter_code = 'CLEANING_ROOM_TEMPERATURE'
    OR rule.parameter_name IN ('室温', '清洗温度')
    OR rule.standard_text = '室温'
  );

UPDATE mes_pro_process_pool_device_parameter_rule rule
JOIN mes_pro_route_process route_process
  ON route_process.id = rule.route_process_id
 AND route_process.tenant_id = rule.tenant_id
 AND route_process.process_id = rule.process_id
 AND route_process.deleted = b'0'
JOIN mes_pro_process process
  ON process.id = rule.process_id
 AND process.tenant_id = rule.tenant_id
 AND process.deleted = b'0'
JOIN mes_pro_process_pool_team_device device
  ON device.id = rule.device_id
 AND device.tenant_id = rule.tenant_id
 AND device.deleted = b'0'
SET
  rule.parameter_code = 'CLEANING_TIME',
  rule.parameter_name = '清洗时间',
  rule.updater = '20260811_cleaning_process_parameter_data',
  rule.update_time = NOW()
WHERE rule.deleted = b'0'
  AND process.`name` = '清洗工序'
  AND device.device_name LIKE '%超声波清洗机%'
  AND (rule.parameter_code = 'CLEANING_TIME' OR rule.parameter_name = '清洗时间');

COMMIT;

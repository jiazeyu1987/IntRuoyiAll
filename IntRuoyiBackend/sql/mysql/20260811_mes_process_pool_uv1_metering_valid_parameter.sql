-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260810_mes_process_pool_device_parameter_select_options; type=data; riskLevel=medium
-- Restore A05059 from the formal A05075 UV curing I configuration, then add METERING_VALID to both devices.
-- Recovery: preflight failures leave business data unchanged. Fix the reported formal configuration conflict, then retry.
-- Recovery: retain the pre-migration database backup. Rollback is allowed only before any frontline submission references these rows.
-- Rollback scope: rows created by creator 20260811_uv1_metering_valid must be reviewed in dependency order before removal.

DROP PROCEDURE IF EXISTS preflight_mes_pp_uv1_metering_valid_parameter;
DROP PROCEDURE IF EXISTS postflight_mes_pp_uv1_metering_valid_parameter;
DROP PROCEDURE IF EXISTS apply_mes_pp_uv1_metering_valid_parameter;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_uv1_metering_valid_parameter()
BEGIN
  DECLARE v_source_scope_count bigint DEFAULT 0;

  IF (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN (
        'mes_pro_process',
        'mes_pro_process_pool_team_device',
        'mes_pro_process_pool_team_process_device',
        'mes_pro_process_pool_device_parameter_rule'
      )
  ) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES UV curing I device configuration tables';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name IN (
        'route_process_id',
        'default_value',
        'value_type',
        'standard_text',
        'option_values_json',
        'default_text',
        'decimal_scale'
      )
  ) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing typed MES device parameter rule columns';
  END IF;

  SELECT COUNT(DISTINCT
    source_rule.`tenant_id`,
    source_rule.`leader_user_id`,
    source_rule.`route_process_id`,
    source_rule.`process_id`
  )
  INTO v_source_scope_count
  FROM `mes_pro_process_pool_device_parameter_rule` source_rule
  JOIN `mes_pro_process_pool_team_device` source_device
    ON source_device.`id` = source_rule.`device_id`
   AND source_device.`tenant_id` = source_rule.`tenant_id`
   AND source_device.`leader_user_id` = source_rule.`leader_user_id`
   AND source_device.`deleted` = b'0'
   AND source_device.`enabled` = b'1'
   AND source_device.`device_status` = 'ENABLED'
   AND source_device.`device_code` = 'A05075'
  JOIN `mes_pro_process_pool_team_process_device` source_binding
    ON source_binding.`tenant_id` = source_rule.`tenant_id`
   AND source_binding.`leader_user_id` = source_rule.`leader_user_id`
   AND source_binding.`process_id` = source_rule.`process_id`
   AND source_binding.`device_id` = source_rule.`device_id`
   AND source_binding.`deleted` = b'0'
   AND source_binding.`enabled` = b'1'
  JOIN `mes_pro_process` source_process
    ON source_process.`id` = source_rule.`process_id`
   AND source_process.`tenant_id` = source_rule.`tenant_id`
   AND source_process.`deleted` = b'0'
   AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
  WHERE source_rule.`deleted` = b'0'
    AND source_rule.`enabled` = b'1'
    AND source_rule.`route_process_id` IS NOT NULL
    AND source_rule.`parameter_code` NOT IN (
      'METERING_VALID',
      'METERING_VALIDITY_WITHIN_PERIOD'
    );

  IF v_source_scope_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled A05075 UV curing I source configuration';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` source_rule
    JOIN `mes_pro_process_pool_team_device` source_device
      ON source_device.`id` = source_rule.`device_id`
     AND source_device.`tenant_id` = source_rule.`tenant_id`
     AND source_device.`leader_user_id` = source_rule.`leader_user_id`
     AND source_device.`deleted` = b'0'
     AND source_device.`device_code` = 'A05075'
    JOIN `mes_pro_process` source_process
      ON source_process.`id` = source_rule.`process_id`
     AND source_process.`tenant_id` = source_rule.`tenant_id`
     AND source_process.`deleted` = b'0'
     AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
    WHERE source_rule.`deleted` = b'0'
      AND source_rule.`parameter_code` = 'METERING_VALIDITY_WITHIN_PERIOD'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Legacy UV curing I metering-valid parameter requires governance';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` source_rule
    JOIN `mes_pro_process_pool_team_device` source_device
      ON source_device.`id` = source_rule.`device_id`
     AND source_device.`tenant_id` = source_rule.`tenant_id`
     AND source_device.`leader_user_id` = source_rule.`leader_user_id`
     AND source_device.`deleted` = b'0'
     AND source_device.`enabled` = b'1'
     AND source_device.`device_status` = 'ENABLED'
     AND source_device.`device_code` = 'A05075'
    JOIN `mes_pro_process` source_process
      ON source_process.`id` = source_rule.`process_id`
     AND source_process.`tenant_id` = source_rule.`tenant_id`
     AND source_process.`deleted` = b'0'
     AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
    JOIN `mes_pro_process_pool_team_device` target_device
      ON target_device.`tenant_id` = source_rule.`tenant_id`
     AND target_device.`leader_user_id` = source_rule.`leader_user_id`
     AND target_device.`deleted` = b'0'
     AND target_device.`device_code` = 'A05059'
    WHERE source_rule.`deleted` = b'0'
      AND source_rule.`enabled` = b'1'
      AND (
        target_device.`enabled` <> b'1'
        OR target_device.`device_status` <> 'ENABLED'
        OR target_device.`device_name` <> source_device.`device_name`
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting A05059 team device blocks UV curing I migration';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` source_rule
    JOIN `mes_pro_process_pool_team_device` source_device
      ON source_device.`id` = source_rule.`device_id`
     AND source_device.`tenant_id` = source_rule.`tenant_id`
     AND source_device.`leader_user_id` = source_rule.`leader_user_id`
     AND source_device.`deleted` = b'0'
     AND source_device.`enabled` = b'1'
     AND source_device.`device_status` = 'ENABLED'
     AND source_device.`device_code` = 'A05075'
    JOIN `mes_pro_process` source_process
      ON source_process.`id` = source_rule.`process_id`
     AND source_process.`tenant_id` = source_rule.`tenant_id`
     AND source_process.`deleted` = b'0'
     AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
    JOIN `mes_pro_process_pool_team_device` target_device
      ON target_device.`tenant_id` = source_rule.`tenant_id`
     AND target_device.`leader_user_id` = source_rule.`leader_user_id`
     AND target_device.`deleted` = b'0'
     AND target_device.`device_code` = 'A05059'
    JOIN `mes_pro_process_pool_team_process_device` target_binding
      ON target_binding.`tenant_id` = source_rule.`tenant_id`
     AND target_binding.`leader_user_id` = source_rule.`leader_user_id`
     AND target_binding.`process_id` = source_rule.`process_id`
     AND target_binding.`device_id` = target_device.`id`
     AND target_binding.`deleted` = b'0'
    WHERE source_rule.`deleted` = b'0'
      AND source_rule.`enabled` = b'1'
      AND target_binding.`enabled` <> b'1'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Disabled A05059 UV curing I binding blocks migration';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` source_rule
    JOIN `mes_pro_process_pool_team_device` source_device
      ON source_device.`id` = source_rule.`device_id`
     AND source_device.`tenant_id` = source_rule.`tenant_id`
     AND source_device.`leader_user_id` = source_rule.`leader_user_id`
     AND source_device.`deleted` = b'0'
     AND source_device.`enabled` = b'1'
     AND source_device.`device_status` = 'ENABLED'
     AND source_device.`device_code` = 'A05075'
    JOIN `mes_pro_process` source_process
      ON source_process.`id` = source_rule.`process_id`
     AND source_process.`tenant_id` = source_rule.`tenant_id`
     AND source_process.`deleted` = b'0'
     AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
    JOIN `mes_pro_process_pool_team_device` target_device
      ON target_device.`tenant_id` = source_rule.`tenant_id`
     AND target_device.`leader_user_id` = source_rule.`leader_user_id`
     AND target_device.`deleted` = b'0'
     AND target_device.`device_code` = 'A05059'
    JOIN `mes_pro_process_pool_device_parameter_rule` target_rule
      ON target_rule.`tenant_id` = source_rule.`tenant_id`
     AND target_rule.`route_process_id` = source_rule.`route_process_id`
     AND target_rule.`device_id` = target_device.`id`
     AND target_rule.`parameter_code` = REPLACE(source_rule.`parameter_code`, 'A05075', 'A05059')
     AND target_rule.`deleted` = b'0'
    WHERE source_rule.`deleted` = b'0'
      AND source_rule.`enabled` = b'1'
      AND source_rule.`parameter_code` NOT IN (
        'METERING_VALID',
        'METERING_VALIDITY_WITHIN_PERIOD'
      )
      AND (
        target_rule.`leader_user_id` <> source_rule.`leader_user_id`
        OR target_rule.`process_id` <> source_rule.`process_id`
        OR NOT (target_rule.`parameter_name` <=> source_rule.`parameter_name`)
        OR NOT (target_rule.`unit` <=> source_rule.`unit`)
        OR NOT (target_rule.`lower_limit` <=> source_rule.`lower_limit`)
        OR NOT (target_rule.`upper_limit` <=> source_rule.`upper_limit`)
        OR NOT (target_rule.`default_value` <=> source_rule.`default_value`)
        OR NOT (target_rule.`value_type` <=> source_rule.`value_type`)
        OR NOT (target_rule.`standard_text` <=> source_rule.`standard_text`)
        OR NOT (target_rule.`option_values_json` <=> source_rule.`option_values_json`)
        OR NOT (target_rule.`default_text` <=> source_rule.`default_text`)
        OR NOT (target_rule.`decimal_scale` <=> source_rule.`decimal_scale`)
        OR target_rule.`enabled` <> b'1'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting A05059 UV curing I parameter rule blocks migration';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` metering_rule
    JOIN `mes_pro_process` target_process
      ON target_process.`id` = metering_rule.`process_id`
     AND target_process.`tenant_id` = metering_rule.`tenant_id`
     AND target_process.`deleted` = b'0'
     AND target_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
    JOIN `mes_pro_process_pool_team_device` target_device
      ON target_device.`id` = metering_rule.`device_id`
     AND target_device.`tenant_id` = metering_rule.`tenant_id`
     AND target_device.`deleted` = b'0'
     AND target_device.`device_code` IN ('A05075', 'A05059')
    WHERE metering_rule.`deleted` = b'0'
      AND metering_rule.`parameter_code` = 'METERING_VALID'
      AND (
        NOT (metering_rule.`parameter_name` <=> '在计量效期内')
        OR NOT (metering_rule.`value_type` <=> 'BOOLEAN')
        OR NOT (metering_rule.`default_value` <=> 0)
        OR metering_rule.`lower_limit` IS NOT NULL
        OR metering_rule.`upper_limit` IS NOT NULL
        OR metering_rule.`option_values_json` IS NOT NULL
        OR metering_rule.`default_text` IS NOT NULL
        OR metering_rule.`decimal_scale` IS NOT NULL
        OR NOT (metering_rule.`standard_text` <=> '是否在计量效期内')
        OR metering_rule.`enabled` <> b'1'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting UV curing I METERING_VALID rule blocks migration';
  END IF;
END$$

CREATE PROCEDURE postflight_mes_pp_uv1_metering_valid_parameter()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT
        source_rule.`tenant_id`,
        source_rule.`leader_user_id`,
        source_rule.`route_process_id`,
        source_rule.`process_id`,
        COUNT(DISTINCT target_device.`device_code`) AS target_device_count
      FROM `mes_pro_process_pool_device_parameter_rule` source_rule
      JOIN `mes_pro_process_pool_team_device` source_device
        ON source_device.`id` = source_rule.`device_id`
       AND source_device.`tenant_id` = source_rule.`tenant_id`
       AND source_device.`leader_user_id` = source_rule.`leader_user_id`
       AND source_device.`deleted` = b'0'
       AND source_device.`enabled` = b'1'
       AND source_device.`device_status` = 'ENABLED'
       AND source_device.`device_code` = 'A05075'
      JOIN `mes_pro_process` source_process
        ON source_process.`id` = source_rule.`process_id`
       AND source_process.`tenant_id` = source_rule.`tenant_id`
       AND source_process.`deleted` = b'0'
       AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
      JOIN `mes_pro_process_pool_team_device` target_device
        ON target_device.`tenant_id` = source_rule.`tenant_id`
       AND target_device.`leader_user_id` = source_rule.`leader_user_id`
       AND target_device.`deleted` = b'0'
       AND target_device.`enabled` = b'1'
       AND target_device.`device_status` = 'ENABLED'
       AND target_device.`device_code` IN ('A05075', 'A05059')
      JOIN `mes_pro_process_pool_team_process_device` target_binding
        ON target_binding.`tenant_id` = source_rule.`tenant_id`
       AND target_binding.`leader_user_id` = source_rule.`leader_user_id`
       AND target_binding.`process_id` = source_rule.`process_id`
       AND target_binding.`device_id` = target_device.`id`
       AND target_binding.`deleted` = b'0'
       AND target_binding.`enabled` = b'1'
      WHERE source_rule.`deleted` = b'0'
        AND source_rule.`enabled` = b'1'
        AND source_rule.`route_process_id` IS NOT NULL
        AND source_rule.`parameter_code` NOT IN (
          'METERING_VALID',
          'METERING_VALIDITY_WITHIN_PERIOD'
        )
      GROUP BY
        source_rule.`tenant_id`,
        source_rule.`leader_user_id`,
        source_rule.`route_process_id`,
        source_rule.`process_id`
      HAVING COUNT(DISTINCT target_device.`device_code`) <> 2
    ) missing_target_device
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UV curing I target device or binding postflight failed';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` source_rule
    JOIN `mes_pro_process_pool_team_device` source_device
      ON source_device.`id` = source_rule.`device_id`
     AND source_device.`tenant_id` = source_rule.`tenant_id`
     AND source_device.`leader_user_id` = source_rule.`leader_user_id`
     AND source_device.`deleted` = b'0'
     AND source_device.`enabled` = b'1'
     AND source_device.`device_status` = 'ENABLED'
     AND source_device.`device_code` = 'A05075'
    JOIN `mes_pro_process` source_process
      ON source_process.`id` = source_rule.`process_id`
     AND source_process.`tenant_id` = source_rule.`tenant_id`
     AND source_process.`deleted` = b'0'
     AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
    JOIN `mes_pro_process_pool_team_device` target_device
      ON target_device.`tenant_id` = source_rule.`tenant_id`
     AND target_device.`leader_user_id` = source_rule.`leader_user_id`
     AND target_device.`deleted` = b'0'
     AND target_device.`enabled` = b'1'
     AND target_device.`device_status` = 'ENABLED'
     AND target_device.`device_code` = 'A05059'
    LEFT JOIN `mes_pro_process_pool_device_parameter_rule` target_rule
      ON target_rule.`tenant_id` = source_rule.`tenant_id`
     AND target_rule.`route_process_id` = source_rule.`route_process_id`
     AND target_rule.`device_id` = target_device.`id`
     AND target_rule.`parameter_code` = REPLACE(source_rule.`parameter_code`, 'A05075', 'A05059')
     AND target_rule.`deleted` = b'0'
    WHERE source_rule.`deleted` = b'0'
      AND source_rule.`enabled` = b'1'
      AND source_rule.`route_process_id` IS NOT NULL
      AND source_rule.`parameter_code` NOT IN (
        'METERING_VALID',
        'METERING_VALIDITY_WITHIN_PERIOD'
      )
      AND (
        target_rule.`id` IS NULL
        OR target_rule.`leader_user_id` <> source_rule.`leader_user_id`
        OR target_rule.`process_id` <> source_rule.`process_id`
        OR NOT (target_rule.`parameter_name` <=> source_rule.`parameter_name`)
        OR NOT (target_rule.`unit` <=> source_rule.`unit`)
        OR NOT (target_rule.`lower_limit` <=> source_rule.`lower_limit`)
        OR NOT (target_rule.`upper_limit` <=> source_rule.`upper_limit`)
        OR NOT (target_rule.`default_value` <=> source_rule.`default_value`)
        OR NOT (target_rule.`value_type` <=> source_rule.`value_type`)
        OR NOT (target_rule.`standard_text` <=> source_rule.`standard_text`)
        OR NOT (target_rule.`option_values_json` <=> source_rule.`option_values_json`)
        OR NOT (target_rule.`default_text` <=> source_rule.`default_text`)
        OR NOT (target_rule.`decimal_scale` <=> source_rule.`decimal_scale`)
        OR target_rule.`enabled` <> b'1'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'A05059 UV curing I parameter clone postflight failed';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT
        source_rule.`tenant_id`,
        source_rule.`leader_user_id`,
        source_rule.`route_process_id`,
        source_rule.`process_id`,
        COUNT(DISTINCT target_device.`device_code`) AS metering_device_count
      FROM `mes_pro_process_pool_device_parameter_rule` source_rule
      JOIN `mes_pro_process_pool_team_device` source_device
        ON source_device.`id` = source_rule.`device_id`
       AND source_device.`tenant_id` = source_rule.`tenant_id`
       AND source_device.`leader_user_id` = source_rule.`leader_user_id`
       AND source_device.`deleted` = b'0'
       AND source_device.`enabled` = b'1'
       AND source_device.`device_status` = 'ENABLED'
       AND source_device.`device_code` = 'A05075'
      JOIN `mes_pro_process` source_process
        ON source_process.`id` = source_rule.`process_id`
       AND source_process.`tenant_id` = source_rule.`tenant_id`
       AND source_process.`deleted` = b'0'
       AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
      JOIN `mes_pro_process_pool_team_device` target_device
        ON target_device.`tenant_id` = source_rule.`tenant_id`
       AND target_device.`leader_user_id` = source_rule.`leader_user_id`
       AND target_device.`deleted` = b'0'
       AND target_device.`enabled` = b'1'
       AND target_device.`device_status` = 'ENABLED'
       AND target_device.`device_code` IN ('A05075', 'A05059')
      JOIN `mes_pro_process_pool_device_parameter_rule` metering_rule
        ON metering_rule.`tenant_id` = source_rule.`tenant_id`
       AND metering_rule.`leader_user_id` = source_rule.`leader_user_id`
       AND metering_rule.`route_process_id` = source_rule.`route_process_id`
       AND metering_rule.`process_id` = source_rule.`process_id`
       AND metering_rule.`device_id` = target_device.`id`
       AND metering_rule.`parameter_code` = 'METERING_VALID'
       AND metering_rule.`parameter_name` = '在计量效期内'
       AND metering_rule.`value_type` = 'BOOLEAN'
       AND metering_rule.`default_value` = 0
       AND metering_rule.`lower_limit` IS NULL
       AND metering_rule.`upper_limit` IS NULL
       AND metering_rule.`standard_text` = '是否在计量效期内'
       AND metering_rule.`option_values_json` IS NULL
       AND metering_rule.`default_text` IS NULL
       AND metering_rule.`decimal_scale` IS NULL
       AND metering_rule.`deleted` = b'0'
       AND metering_rule.`enabled` = b'1'
      WHERE source_rule.`deleted` = b'0'
        AND source_rule.`enabled` = b'1'
        AND source_rule.`route_process_id` IS NOT NULL
        AND source_rule.`parameter_code` NOT IN (
          'METERING_VALID',
          'METERING_VALIDITY_WITHIN_PERIOD'
        )
      GROUP BY
        source_rule.`tenant_id`,
        source_rule.`leader_user_id`,
        source_rule.`route_process_id`,
        source_rule.`process_id`
      HAVING COUNT(DISTINCT target_device.`device_code`) <> 2
    ) missing_metering_rule
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UV curing I METERING_VALID postflight failed';
  END IF;
END$$

CREATE PROCEDURE apply_mes_pp_uv1_metering_valid_parameter()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

INSERT INTO `mes_pro_process_pool_team_device` (
  `leader_user_id`,
  `device_code`,
  `device_name`,
  `device_status`,
  `enabled`,
  `status_changed_at`,
  `remark`,
  `creator`,
  `create_time`,
  `updater`,
  `update_time`,
  `deleted`,
  `tenant_id`
)
SELECT DISTINCT
  source_device.`leader_user_id`,
  'A05059',
  source_device.`device_name`,
  'ENABLED',
  b'1',
  NOW(),
  '由光固Ⅰ A05075 正式配置补齐',
  '20260811_uv1_metering_valid',
  NOW(),
  '20260811_uv1_metering_valid',
  NOW(),
  b'0',
  source_device.`tenant_id`
FROM `mes_pro_process_pool_team_device` source_device
JOIN `mes_pro_process_pool_team_process_device` source_binding
  ON source_binding.`tenant_id` = source_device.`tenant_id`
 AND source_binding.`leader_user_id` = source_device.`leader_user_id`
 AND source_binding.`device_id` = source_device.`id`
 AND source_binding.`deleted` = b'0'
 AND source_binding.`enabled` = b'1'
JOIN `mes_pro_process` source_process
  ON source_process.`id` = source_binding.`process_id`
 AND source_process.`tenant_id` = source_device.`tenant_id`
 AND source_process.`deleted` = b'0'
 AND source_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
JOIN `mes_pro_process_pool_device_parameter_rule` source_rule
  ON source_rule.`tenant_id` = source_device.`tenant_id`
 AND source_rule.`leader_user_id` = source_device.`leader_user_id`
 AND source_rule.`process_id` = source_binding.`process_id`
 AND source_rule.`device_id` = source_device.`id`
 AND source_rule.`deleted` = b'0'
 AND source_rule.`enabled` = b'1'
 AND source_rule.`route_process_id` IS NOT NULL
WHERE source_device.`deleted` = b'0'
  AND source_device.`enabled` = b'1'
  AND source_device.`device_status` = 'ENABLED'
  AND source_device.`device_code` = 'A05075'
  AND source_rule.`parameter_code` NOT IN (
    'METERING_VALID',
    'METERING_VALIDITY_WITHIN_PERIOD'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_team_device` existing_device
    WHERE existing_device.`tenant_id` = source_device.`tenant_id`
      AND existing_device.`leader_user_id` = source_device.`leader_user_id`
      AND existing_device.`device_code` = 'A05059'
      AND existing_device.`deleted` = b'0'
  );

INSERT INTO `mes_pro_process_pool_team_process_device` (
  `leader_user_id`,
  `process_id`,
  `device_id`,
  `enabled`,
  `disabled_at`,
  `remark`,
  `creator`,
  `create_time`,
  `updater`,
  `update_time`,
  `deleted`,
  `tenant_id`
)
SELECT DISTINCT
  source_binding.`leader_user_id`,
  source_binding.`process_id`,
  target_device.`id`,
  b'1',
  NULL,
  'A05059 仅绑定光固Ⅰ正式工序',
  '20260811_uv1_metering_valid',
  NOW(),
  '20260811_uv1_metering_valid',
  NOW(),
  b'0',
  source_binding.`tenant_id`
FROM `mes_pro_process_pool_team_process_device` source_binding
JOIN `mes_pro_process_pool_team_device` source_device
  ON source_device.`id` = source_binding.`device_id`
 AND source_device.`tenant_id` = source_binding.`tenant_id`
 AND source_device.`leader_user_id` = source_binding.`leader_user_id`
 AND source_device.`deleted` = b'0'
 AND source_device.`enabled` = b'1'
 AND source_device.`device_status` = 'ENABLED'
 AND source_device.`device_code` = 'A05075'
JOIN `mes_pro_process_pool_team_device` target_device
  ON target_device.`tenant_id` = source_binding.`tenant_id`
 AND target_device.`leader_user_id` = source_binding.`leader_user_id`
 AND target_device.`deleted` = b'0'
 AND target_device.`enabled` = b'1'
 AND target_device.`device_status` = 'ENABLED'
 AND target_device.`device_code` = 'A05059'
JOIN `mes_pro_process` target_process
  ON target_process.`id` = source_binding.`process_id`
 AND target_process.`tenant_id` = source_binding.`tenant_id`
 AND target_process.`deleted` = b'0'
 AND target_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
WHERE source_binding.`deleted` = b'0'
  AND source_binding.`enabled` = b'1'
  AND NOT EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_team_process_device` existing_binding
    WHERE existing_binding.`tenant_id` = source_binding.`tenant_id`
      AND existing_binding.`leader_user_id` = source_binding.`leader_user_id`
      AND existing_binding.`process_id` = source_binding.`process_id`
      AND existing_binding.`device_id` = target_device.`id`
      AND existing_binding.`deleted` = b'0'
  );

INSERT INTO `mes_pro_process_pool_device_parameter_rule` (
  `leader_user_id`,
  `route_process_id`,
  `process_id`,
  `device_id`,
  `parameter_code`,
  `parameter_name`,
  `unit`,
  `lower_limit`,
  `upper_limit`,
  `default_value`,
  `value_type`,
  `standard_text`,
  `option_values_json`,
  `default_text`,
  `decimal_scale`,
  `enabled`,
  `remark`,
  `creator`,
  `create_time`,
  `updater`,
  `update_time`,
  `deleted`,
  `tenant_id`
)
SELECT
  source_rule.`leader_user_id`,
  source_rule.`route_process_id`,
  source_rule.`process_id`,
  target_device.`id`,
  REPLACE(source_rule.`parameter_code`, 'A05075', 'A05059'),
  source_rule.`parameter_name`,
  source_rule.`unit`,
  source_rule.`lower_limit`,
  source_rule.`upper_limit`,
  source_rule.`default_value`,
  source_rule.`value_type`,
  source_rule.`standard_text`,
  source_rule.`option_values_json`,
  source_rule.`default_text`,
  source_rule.`decimal_scale`,
  b'1',
  '由光固Ⅰ A05075 正式参数映射',
  '20260811_uv1_metering_valid',
  NOW(),
  '20260811_uv1_metering_valid',
  NOW(),
  b'0',
  source_rule.`tenant_id`
FROM `mes_pro_process_pool_device_parameter_rule` source_rule
JOIN `mes_pro_process_pool_team_device` source_device
  ON source_device.`id` = source_rule.`device_id`
 AND source_device.`tenant_id` = source_rule.`tenant_id`
 AND source_device.`leader_user_id` = source_rule.`leader_user_id`
 AND source_device.`deleted` = b'0'
 AND source_device.`enabled` = b'1'
 AND source_device.`device_status` = 'ENABLED'
 AND source_device.`device_code` = 'A05075'
JOIN `mes_pro_process_pool_team_device` target_device
  ON target_device.`tenant_id` = source_rule.`tenant_id`
 AND target_device.`leader_user_id` = source_rule.`leader_user_id`
 AND target_device.`deleted` = b'0'
 AND target_device.`enabled` = b'1'
 AND target_device.`device_status` = 'ENABLED'
 AND target_device.`device_code` = 'A05059'
JOIN `mes_pro_process` target_process
  ON target_process.`id` = source_rule.`process_id`
 AND target_process.`tenant_id` = source_rule.`tenant_id`
 AND target_process.`deleted` = b'0'
 AND target_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
JOIN `mes_pro_process_pool_team_process_device` source_binding
  ON source_binding.`tenant_id` = source_rule.`tenant_id`
 AND source_binding.`leader_user_id` = source_rule.`leader_user_id`
 AND source_binding.`process_id` = source_rule.`process_id`
 AND source_binding.`device_id` = source_device.`id`
 AND source_binding.`deleted` = b'0'
 AND source_binding.`enabled` = b'1'
JOIN `mes_pro_process_pool_team_process_device` target_binding
  ON target_binding.`tenant_id` = source_rule.`tenant_id`
 AND target_binding.`leader_user_id` = source_rule.`leader_user_id`
 AND target_binding.`process_id` = source_rule.`process_id`
 AND target_binding.`device_id` = target_device.`id`
 AND target_binding.`deleted` = b'0'
 AND target_binding.`enabled` = b'1'
WHERE source_rule.`deleted` = b'0'
  AND source_rule.`enabled` = b'1'
  AND source_rule.`route_process_id` IS NOT NULL
  AND source_rule.`parameter_code` NOT IN (
    'METERING_VALID',
    'METERING_VALIDITY_WITHIN_PERIOD'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` existing_rule
    WHERE existing_rule.`tenant_id` = source_rule.`tenant_id`
      AND existing_rule.`route_process_id` = source_rule.`route_process_id`
      AND existing_rule.`device_id` = target_device.`id`
      AND existing_rule.`parameter_code` = REPLACE(source_rule.`parameter_code`, 'A05075', 'A05059')
      AND existing_rule.`deleted` = b'0'
  );

INSERT INTO `mes_pro_process_pool_device_parameter_rule` (
  `leader_user_id`,
  `route_process_id`,
  `process_id`,
  `device_id`,
  `parameter_code`,
  `parameter_name`,
  `unit`,
  `lower_limit`,
  `upper_limit`,
  `default_value`,
  `value_type`,
  `standard_text`,
  `option_values_json`,
  `default_text`,
  `decimal_scale`,
  `enabled`,
  `remark`,
  `creator`,
  `create_time`,
  `updater`,
  `update_time`,
  `deleted`,
  `tenant_id`
)
SELECT DISTINCT
  source_rule.`leader_user_id`,
  source_rule.`route_process_id`,
  source_rule.`process_id`,
  target_device.`id`,
  'METERING_VALID',
  '在计量效期内',
  NULL,
  NULL,
  NULL,
  0,
  'BOOLEAN',
  '是否在计量效期内',
  NULL,
  NULL,
  NULL,
  b'1',
  '光固Ⅰ设备计量有效期确认',
  '20260811_uv1_metering_valid',
  NOW(),
  '20260811_uv1_metering_valid',
  NOW(),
  b'0',
  source_rule.`tenant_id`
FROM `mes_pro_process_pool_device_parameter_rule` source_rule
JOIN `mes_pro_process_pool_team_device` source_device
  ON source_device.`id` = source_rule.`device_id`
 AND source_device.`tenant_id` = source_rule.`tenant_id`
 AND source_device.`leader_user_id` = source_rule.`leader_user_id`
 AND source_device.`deleted` = b'0'
 AND source_device.`enabled` = b'1'
 AND source_device.`device_status` = 'ENABLED'
 AND source_device.`device_code` = 'A05075'
JOIN `mes_pro_process_pool_team_device` target_device
  ON target_device.`tenant_id` = source_rule.`tenant_id`
 AND target_device.`leader_user_id` = source_rule.`leader_user_id`
 AND target_device.`deleted` = b'0'
 AND target_device.`enabled` = b'1'
 AND target_device.`device_status` = 'ENABLED'
 AND target_device.`device_code` IN ('A05075', 'A05059')
JOIN `mes_pro_process` target_process
  ON target_process.`id` = source_rule.`process_id`
 AND target_process.`tenant_id` = source_rule.`tenant_id`
 AND target_process.`deleted` = b'0'
 AND target_process.`name` IN ('光固Ⅰ', '光固Ⅰ工序')
JOIN `mes_pro_process_pool_team_process_device` target_binding
  ON target_binding.`tenant_id` = source_rule.`tenant_id`
 AND target_binding.`leader_user_id` = source_rule.`leader_user_id`
 AND target_binding.`process_id` = source_rule.`process_id`
 AND target_binding.`device_id` = target_device.`id`
 AND target_binding.`deleted` = b'0'
 AND target_binding.`enabled` = b'1'
WHERE source_rule.`deleted` = b'0'
  AND source_rule.`enabled` = b'1'
  AND source_rule.`route_process_id` IS NOT NULL
  AND source_rule.`parameter_code` NOT IN (
    'METERING_VALID',
    'METERING_VALIDITY_WITHIN_PERIOD'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` existing_rule
    WHERE existing_rule.`tenant_id` = source_rule.`tenant_id`
      AND existing_rule.`route_process_id` = source_rule.`route_process_id`
      AND existing_rule.`device_id` = target_device.`id`
      AND existing_rule.`parameter_code` = 'METERING_VALID'
      AND existing_rule.`deleted` = b'0'
  );

  CALL postflight_mes_pp_uv1_metering_valid_parameter();

  COMMIT;
END$$
DELIMITER ;

CALL preflight_mes_pp_uv1_metering_valid_parameter();
CALL apply_mes_pp_uv1_metering_valid_parameter();

DROP PROCEDURE IF EXISTS preflight_mes_pp_uv1_metering_valid_parameter;
DROP PROCEDURE IF EXISTS postflight_mes_pp_uv1_metering_valid_parameter;
DROP PROCEDURE IF EXISTS apply_mes_pp_uv1_metering_valid_parameter;

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260810_mes_process_pool_device_parameter_select_options; type=data; riskLevel=medium
-- Complete the formal A05075/A05059 device set and parameter rules for the UV curing II process.
-- Recovery: if preflight, migration, or postflight SIGNAL fails, the migration procedure rolls back and stops deployment.
-- Rollback: mark only rows created by creator 20260811_uv2_two_devices as deleted after dependency review.
-- Rollback: restore any re-enabled pre-existing A05059 binding from the mandatory pre-migration database backup.
-- Rollback blocker: do not roll back after frontline submissions reference the migrated device parameter rules.

DROP PROCEDURE IF EXISTS preflight_mes_pp_uv2_two_device_runtime_config;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_uv2_two_device_runtime_config()
BEGIN
  DECLARE v_source_binding_count int DEFAULT 0;
  DECLARE v_missing_target_device_count int DEFAULT 0;
  DECLARE v_missing_source_rule_count int DEFAULT 0;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_team_process_device'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES process device binding table';
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

  SELECT COUNT(*)
  INTO v_source_binding_count
  FROM `mes_pro_process_pool_team_process_device` source_binding
  JOIN `mes_pro_process` process
    ON process.`id` = source_binding.`process_id`
   AND process.`tenant_id` = source_binding.`tenant_id`
   AND process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` source_device
    ON source_device.`id` = source_binding.`device_id`
   AND source_device.`leader_user_id` = source_binding.`leader_user_id`
   AND source_device.`tenant_id` = source_binding.`tenant_id`
   AND source_device.`deleted` = b'0'
  WHERE source_binding.`deleted` = b'0'
    AND source_binding.`enabled` = b'1'
    AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
    AND source_device.`device_code` = 'A05075'
    AND source_device.`enabled` = b'1'
    AND source_device.`device_status` = 'ENABLED';

  IF v_source_binding_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UV curing II requires an enabled formal A05075 source binding';
  END IF;

  SELECT COUNT(*)
  INTO v_missing_target_device_count
  FROM `mes_pro_process_pool_team_process_device` source_binding
  JOIN `mes_pro_process` process
    ON process.`id` = source_binding.`process_id`
   AND process.`tenant_id` = source_binding.`tenant_id`
   AND process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` source_device
    ON source_device.`id` = source_binding.`device_id`
   AND source_device.`leader_user_id` = source_binding.`leader_user_id`
   AND source_device.`tenant_id` = source_binding.`tenant_id`
   AND source_device.`deleted` = b'0'
  WHERE source_binding.`deleted` = b'0'
    AND source_binding.`enabled` = b'1'
    AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
    AND source_device.`device_code` = 'A05075'
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_process_pool_team_device` target_device
      WHERE target_device.`tenant_id` = source_binding.`tenant_id`
        AND target_device.`leader_user_id` = source_binding.`leader_user_id`
        AND target_device.`device_code` = 'A05059'
        AND target_device.`device_status` = 'ENABLED'
        AND target_device.`enabled` = b'1'
        AND target_device.`deleted` = b'0'
    );

  IF v_missing_target_device_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UV curing II A05075 owner is missing enabled A05059 team device';
  END IF;

  SELECT COUNT(*)
  INTO v_missing_source_rule_count
  FROM `mes_pro_process_pool_team_process_device` source_binding
  JOIN `mes_pro_process` process
    ON process.`id` = source_binding.`process_id`
   AND process.`tenant_id` = source_binding.`tenant_id`
   AND process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` source_device
    ON source_device.`id` = source_binding.`device_id`
   AND source_device.`leader_user_id` = source_binding.`leader_user_id`
   AND source_device.`tenant_id` = source_binding.`tenant_id`
   AND source_device.`deleted` = b'0'
  WHERE source_binding.`deleted` = b'0'
    AND source_binding.`enabled` = b'1'
    AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
    AND source_device.`device_code` = 'A05075'
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_process_pool_device_parameter_rule` source_rule
      WHERE source_rule.`tenant_id` = source_binding.`tenant_id`
        AND source_rule.`leader_user_id` = source_binding.`leader_user_id`
        AND source_rule.`process_id` = source_binding.`process_id`
        AND source_rule.`device_id` = source_binding.`device_id`
        AND source_rule.`route_process_id` IS NOT NULL
        AND source_rule.`enabled` = b'1'
        AND source_rule.`deleted` = b'0'
    );

  IF v_missing_source_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UV curing II A05075 binding is missing formal route process parameter rules';
  END IF;

  IF EXISTS (
    SELECT 1
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
      AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
      AND device.`device_code` IN ('A05075', 'A05059')
      AND rule.`parameter_code` = 'METERING_VALID'
      AND (
        NOT (rule.`parameter_name` <=> '在计量效期内')
        OR NOT (rule.`value_type` <=> 'BOOLEAN')
        OR rule.`default_value` IS NULL
        OR rule.`default_value` NOT IN (0, 1)
        OR rule.`lower_limit` IS NOT NULL
        OR rule.`upper_limit` IS NOT NULL
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting UV curing II METERING_VALID rule exists';
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS postflight_mes_pp_uv2_two_device_runtime_config;
DELIMITER $$
CREATE PROCEDURE postflight_mes_pp_uv2_two_device_runtime_config()
BEGIN
  DECLARE v_invalid_binding_group_count int DEFAULT 0;
  DECLARE v_invalid_metering_rule_count int DEFAULT 0;

  SELECT COUNT(*)
  INTO v_invalid_binding_group_count
  FROM (
    SELECT
      binding.`tenant_id`,
      binding.`leader_user_id`,
      binding.`process_id`,
      COUNT(DISTINCT device.`device_code`) AS device_code_count
    FROM `mes_pro_process_pool_team_process_device` binding
    JOIN `mes_pro_process` process
      ON process.`id` = binding.`process_id`
     AND process.`tenant_id` = binding.`tenant_id`
     AND process.`deleted` = b'0'
    JOIN `mes_pro_process_pool_team_device` device
      ON device.`id` = binding.`device_id`
     AND device.`leader_user_id` = binding.`leader_user_id`
     AND device.`tenant_id` = binding.`tenant_id`
     AND device.`deleted` = b'0'
    WHERE binding.`deleted` = b'0'
      AND binding.`enabled` = b'1'
      AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
      AND device.`device_code` IN ('A05075', 'A05059')
      AND EXISTS (
        SELECT 1
        FROM `mes_pro_process_pool_team_process_device` source_binding
        JOIN `mes_pro_process_pool_team_device` source_device
          ON source_device.`id` = source_binding.`device_id`
         AND source_device.`leader_user_id` = source_binding.`leader_user_id`
         AND source_device.`tenant_id` = source_binding.`tenant_id`
         AND source_device.`deleted` = b'0'
        WHERE source_binding.`tenant_id` = binding.`tenant_id`
          AND source_binding.`leader_user_id` = binding.`leader_user_id`
          AND source_binding.`process_id` = binding.`process_id`
          AND source_binding.`enabled` = b'1'
          AND source_binding.`deleted` = b'0'
          AND source_device.`device_code` = 'A05075'
      )
    GROUP BY binding.`tenant_id`, binding.`leader_user_id`, binding.`process_id`
    HAVING COUNT(DISTINCT device.`device_code`) <> 2
  ) invalid_binding_group;

  IF v_invalid_binding_group_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UV curing II formal runtime device set must contain A05075 and A05059';
  END IF;

  SELECT COUNT(*)
  INTO v_invalid_metering_rule_count
  FROM (
    SELECT
      rule.`tenant_id`,
      rule.`leader_user_id`,
      rule.`route_process_id`,
      COUNT(DISTINCT device.`device_code`) AS device_code_count
    FROM `mes_pro_process_pool_device_parameter_rule` rule
    JOIN `mes_pro_process` process
      ON process.`id` = rule.`process_id`
     AND process.`tenant_id` = rule.`tenant_id`
     AND process.`deleted` = b'0'
    JOIN `mes_pro_process_pool_team_device` device
      ON device.`id` = rule.`device_id`
     AND device.`leader_user_id` = rule.`leader_user_id`
     AND device.`tenant_id` = rule.`tenant_id`
     AND device.`deleted` = b'0'
    WHERE rule.`deleted` = b'0'
      AND rule.`enabled` = b'1'
      AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
      AND device.`device_code` IN ('A05075', 'A05059')
      AND rule.`parameter_code` = 'METERING_VALID'
    GROUP BY rule.`tenant_id`, rule.`leader_user_id`, rule.`route_process_id`
    HAVING COUNT(DISTINCT device.`device_code`) <> 2
  ) invalid_metering_rule;

  IF v_invalid_metering_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UV curing II METERING_VALID rules must cover A05075 and A05059';
  END IF;
END$$

DROP PROCEDURE IF EXISTS migrate_mes_pp_uv2_two_device_runtime_config$$
CREATE PROCEDURE migrate_mes_pp_uv2_two_device_runtime_config()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  CALL preflight_mes_pp_uv2_two_device_runtime_config();

  START TRANSACTION;

  UPDATE `mes_pro_process_pool_team_process_device` target_binding
JOIN `mes_pro_process` process
  ON process.`id` = target_binding.`process_id`
 AND process.`tenant_id` = target_binding.`tenant_id`
 AND process.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` target_device
  ON target_device.`id` = target_binding.`device_id`
 AND target_device.`leader_user_id` = target_binding.`leader_user_id`
 AND target_device.`tenant_id` = target_binding.`tenant_id`
 AND target_device.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_process_device` source_binding
  ON source_binding.`tenant_id` = target_binding.`tenant_id`
 AND source_binding.`leader_user_id` = target_binding.`leader_user_id`
 AND source_binding.`process_id` = target_binding.`process_id`
 AND source_binding.`enabled` = b'1'
 AND source_binding.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` source_device
  ON source_device.`id` = source_binding.`device_id`
 AND source_device.`leader_user_id` = source_binding.`leader_user_id`
 AND source_device.`tenant_id` = source_binding.`tenant_id`
 AND source_device.`deleted` = b'0'
SET target_binding.`enabled` = b'1',
    target_binding.`disabled_at` = NULL,
    target_binding.`remark` = '光固Ⅱ双设备正式绑定',
    target_binding.`updater` = '20260811_uv2_two_devices',
    target_binding.`update_time` = NOW()
WHERE target_binding.`deleted` = b'0'
  AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
  AND target_device.`device_code` = 'A05059'
  AND target_device.`device_status` = 'ENABLED'
  AND target_device.`enabled` = b'1'
  AND source_device.`device_code` = 'A05075';

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
  '光固Ⅱ双设备正式绑定',
  '20260811_uv2_two_devices',
  NOW(),
  '20260811_uv2_two_devices',
  NOW(),
  b'0',
  source_binding.`tenant_id`
FROM `mes_pro_process_pool_team_process_device` source_binding
JOIN `mes_pro_process` process
  ON process.`id` = source_binding.`process_id`
 AND process.`tenant_id` = source_binding.`tenant_id`
 AND process.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` source_device
  ON source_device.`id` = source_binding.`device_id`
 AND source_device.`leader_user_id` = source_binding.`leader_user_id`
 AND source_device.`tenant_id` = source_binding.`tenant_id`
 AND source_device.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` target_device
  ON target_device.`leader_user_id` = source_binding.`leader_user_id`
 AND target_device.`tenant_id` = source_binding.`tenant_id`
 AND target_device.`device_code` = 'A05059'
 AND target_device.`device_status` = 'ENABLED'
 AND target_device.`enabled` = b'1'
 AND target_device.`deleted` = b'0'
WHERE source_binding.`enabled` = b'1'
  AND source_binding.`deleted` = b'0'
  AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
  AND source_device.`device_code` = 'A05075'
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
  `value_type`,
  `default_value`,
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
  source_rule.`device_id`,
  'METERING_VALID',
  '在计量效期内',
  NULL,
  NULL,
  NULL,
  'BOOLEAN',
  1,
  '是否在计量效期内',
  NULL,
  NULL,
  NULL,
  b'1',
  '光固Ⅱ设备计量有效期确认',
  '20260811_uv2_two_devices',
  NOW(),
  '20260811_uv2_two_devices',
  NOW(),
  b'0',
  source_rule.`tenant_id`
FROM `mes_pro_process_pool_device_parameter_rule` source_rule
JOIN `mes_pro_process` process
  ON process.`id` = source_rule.`process_id`
 AND process.`tenant_id` = source_rule.`tenant_id`
 AND process.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` source_device
  ON source_device.`id` = source_rule.`device_id`
 AND source_device.`leader_user_id` = source_rule.`leader_user_id`
 AND source_device.`tenant_id` = source_rule.`tenant_id`
 AND source_device.`deleted` = b'0'
WHERE source_rule.`deleted` = b'0'
  AND source_rule.`enabled` = b'1'
  AND source_rule.`route_process_id` IS NOT NULL
  AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
  AND source_device.`device_code` = 'A05075'
  AND NOT EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` existing_rule
    WHERE existing_rule.`tenant_id` = source_rule.`tenant_id`
      AND existing_rule.`route_process_id` = source_rule.`route_process_id`
      AND existing_rule.`device_id` = source_rule.`device_id`
      AND existing_rule.`parameter_code` = 'METERING_VALID'
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
  `value_type`,
  `default_value`,
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
  source_rule.`parameter_code`,
  source_rule.`parameter_name`,
  source_rule.`unit`,
  source_rule.`lower_limit`,
  source_rule.`upper_limit`,
  source_rule.`value_type`,
  source_rule.`default_value`,
  source_rule.`standard_text`,
  source_rule.`option_values_json`,
  source_rule.`default_text`,
  source_rule.`decimal_scale`,
  source_rule.`enabled`,
  '光固Ⅱ A05059 正式参数补齐',
  '20260811_uv2_two_devices',
  NOW(),
  '20260811_uv2_two_devices',
  NOW(),
  b'0',
  source_rule.`tenant_id`
FROM `mes_pro_process_pool_device_parameter_rule` source_rule
JOIN `mes_pro_process` process
  ON process.`id` = source_rule.`process_id`
 AND process.`tenant_id` = source_rule.`tenant_id`
 AND process.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` source_device
  ON source_device.`id` = source_rule.`device_id`
 AND source_device.`leader_user_id` = source_rule.`leader_user_id`
 AND source_device.`tenant_id` = source_rule.`tenant_id`
 AND source_device.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_device` target_device
  ON target_device.`leader_user_id` = source_rule.`leader_user_id`
 AND target_device.`tenant_id` = source_rule.`tenant_id`
 AND target_device.`device_code` = 'A05059'
 AND target_device.`device_status` = 'ENABLED'
 AND target_device.`enabled` = b'1'
 AND target_device.`deleted` = b'0'
JOIN `mes_pro_process_pool_team_process_device` target_binding
  ON target_binding.`leader_user_id` = source_rule.`leader_user_id`
 AND target_binding.`process_id` = source_rule.`process_id`
 AND target_binding.`device_id` = target_device.`id`
 AND target_binding.`tenant_id` = source_rule.`tenant_id`
 AND target_binding.`enabled` = b'1'
 AND target_binding.`deleted` = b'0'
WHERE source_rule.`deleted` = b'0'
  AND source_rule.`enabled` = b'1'
  AND source_rule.`route_process_id` IS NOT NULL
  AND process.`name` IN ('光固Ⅱ', '光固Ⅱ工序')
  AND source_device.`device_code` = 'A05075'
  AND NOT EXISTS (
    SELECT 1
    FROM `mes_pro_process_pool_device_parameter_rule` existing_rule
    WHERE existing_rule.`tenant_id` = source_rule.`tenant_id`
      AND existing_rule.`route_process_id` = source_rule.`route_process_id`
      AND existing_rule.`device_id` = target_device.`id`
      AND existing_rule.`parameter_code` = source_rule.`parameter_code`
      AND existing_rule.`deleted` = b'0'
  );

  CALL postflight_mes_pp_uv2_two_device_runtime_config();

  COMMIT;
END$$
DELIMITER ;

CALL migrate_mes_pp_uv2_two_device_runtime_config();

DROP PROCEDURE IF EXISTS migrate_mes_pp_uv2_two_device_runtime_config;
DROP PROCEDURE IF EXISTS preflight_mes_pp_uv2_two_device_runtime_config;
DROP PROCEDURE IF EXISTS postflight_mes_pp_uv2_two_device_runtime_config;

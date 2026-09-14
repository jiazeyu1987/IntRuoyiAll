-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_process_pool_cleaning_process_parameter_data; type=data; riskLevel=medium
-- 补齐 IDI / 按压式球囊扩充压力泵当前 DCC 路线的生产组长设备参数规则。
-- Recovery: preflight or postflight SIGNAL rolls back the transaction; fix the missing formal source/target configuration and retry.
-- Rollback: soft-delete rows whose creator is 20260830_idi_device_parameter_rules after verifying no production report has consumed them.

DROP PROCEDURE IF EXISTS preflight_mes_pp_idi_device_parameter_rules;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_idi_device_parameter_rules()
BEGIN
  DECLARE v_project_count bigint DEFAULT 0;
  DECLARE v_target_binding_count bigint DEFAULT 0;
  DECLARE v_source_route_count bigint DEFAULT 0;
  DECLARE v_target_device_count bigint DEFAULT 0;
  DECLARE v_missing_source_rule_count bigint DEFAULT 0;
  DECLARE v_duplicate_source_rule_count bigint DEFAULT 0;
  DECLARE v_conflicting_target_rule_count bigint DEFAULT 0;

  IF (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN (
        'dcc_project_code',
        'mes_pro_route_dcc_project_binding',
        'mes_pro_route',
        'mes_pro_route_process',
        'mes_pro_process_pool_team_process_device',
        'mes_pro_process_pool_team_device',
        'mes_pro_process_pool_device_parameter_rule',
        'system_users'
      )
  ) < 8 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing IDI device parameter dependency table';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_device_parameter_rule'
      AND column_name IN (
        'leader_user_id',
        'route_process_id',
        'process_id',
        'device_id',
        'parameter_code',
        'parameter_name',
        'unit',
        'lower_limit',
        'upper_limit',
        'default_value',
        'value_type',
        'standard_text',
        'option_values_json',
        'default_text',
        'decimal_scale',
        'enabled'
      )
  ) < 16 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing typed device parameter rule columns';
  END IF;

  SELECT COUNT(*) INTO v_project_count
  FROM `dcc_project_code` project
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = '按压式球囊扩充压力泵'
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0;

  IF v_project_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected one active IDI DCC project code';
  END IF;

  SELECT COUNT(*) INTO v_target_binding_count
  FROM `dcc_project_code` project
  JOIN `mes_pro_route_dcc_project_binding` binding
    ON binding.`dcc_project_code_id` = project.`id`
   AND binding.`tenant_id` = project.`tenant_id`
   AND binding.`deleted` = b'0'
   AND binding.`active_route_id` = binding.`route_id`
  JOIN `mes_pro_route` target_route
    ON target_route.`id` = binding.`route_id`
   AND target_route.`tenant_id` = project.`tenant_id`
   AND target_route.`deleted` = b'0'
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = '按压式球囊扩充压力泵'
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0
    AND target_route.`code` = 'RT000028-IDI';

  IF v_target_binding_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected one active IDI target route binding';
  END IF;

  SELECT COUNT(*) INTO v_source_route_count
  FROM `mes_pro_route` source_route
  WHERE source_route.`tenant_id` = 1
    AND source_route.`code` = 'RT000028'
    AND source_route.`deleted` = b'0';

  IF v_source_route_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected one active pressure-pump source route';
  END IF;

  SELECT COUNT(*) INTO v_target_device_count
  FROM `dcc_project_code` project
  JOIN `mes_pro_route_dcc_project_binding` binding
    ON binding.`dcc_project_code_id` = project.`id`
   AND binding.`tenant_id` = project.`tenant_id`
   AND binding.`deleted` = b'0'
   AND binding.`active_route_id` = binding.`route_id`
  JOIN `mes_pro_route` target_route
    ON target_route.`id` = binding.`route_id`
   AND target_route.`tenant_id` = project.`tenant_id`
   AND target_route.`deleted` = b'0'
   AND target_route.`code` = 'RT000028-IDI'
  JOIN `mes_pro_route_process` target_route_process
    ON target_route_process.`route_id` = target_route.`id`
   AND target_route_process.`tenant_id` = target_route.`tenant_id`
   AND target_route_process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_process_device` target_binding
    ON target_binding.`tenant_id` = target_route_process.`tenant_id`
   AND target_binding.`process_id` = target_route_process.`process_id`
   AND target_binding.`enabled` = b'1'
   AND target_binding.`deleted` = b'0'
  JOIN `system_users` leader
    ON leader.`id` = target_binding.`leader_user_id`
   AND leader.`tenant_id` = target_binding.`tenant_id`
   AND leader.`username` = 'admin'
   AND leader.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` target_device
    ON target_device.`id` = target_binding.`device_id`
   AND target_device.`leader_user_id` = target_binding.`leader_user_id`
   AND target_device.`tenant_id` = target_binding.`tenant_id`
   AND target_device.`device_status` = 'ENABLED'
   AND target_device.`enabled` = b'1'
   AND target_device.`deleted` = b'0'
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = '按压式球囊扩充压力泵'
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0;

  IF v_target_device_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target route has no active admin device bindings';
  END IF;

  SELECT COUNT(*) INTO v_missing_source_rule_count
  FROM (
    SELECT
      target_route_process.`tenant_id`,
      target_route_process.`id` AS target_route_process_id,
      target_binding.`leader_user_id`,
      target_device.`id` AS target_device_id
    FROM `dcc_project_code` project
    JOIN `mes_pro_route_dcc_project_binding` binding
      ON binding.`dcc_project_code_id` = project.`id`
     AND binding.`tenant_id` = project.`tenant_id`
     AND binding.`deleted` = b'0'
     AND binding.`active_route_id` = binding.`route_id`
    JOIN `mes_pro_route` target_route
      ON target_route.`id` = binding.`route_id`
     AND target_route.`tenant_id` = project.`tenant_id`
     AND target_route.`deleted` = b'0'
     AND target_route.`code` = 'RT000028-IDI'
    JOIN `mes_pro_route_process` target_route_process
      ON target_route_process.`route_id` = target_route.`id`
     AND target_route_process.`tenant_id` = target_route.`tenant_id`
     AND target_route_process.`deleted` = b'0'
    JOIN `mes_pro_process_pool_team_process_device` target_binding
      ON target_binding.`tenant_id` = target_route_process.`tenant_id`
     AND target_binding.`process_id` = target_route_process.`process_id`
     AND target_binding.`enabled` = b'1'
     AND target_binding.`deleted` = b'0'
    JOIN `system_users` leader
      ON leader.`id` = target_binding.`leader_user_id`
     AND leader.`tenant_id` = target_binding.`tenant_id`
     AND leader.`username` = 'admin'
     AND leader.`deleted` = b'0'
    JOIN `mes_pro_process_pool_team_device` target_device
      ON target_device.`id` = target_binding.`device_id`
     AND target_device.`leader_user_id` = target_binding.`leader_user_id`
     AND target_device.`tenant_id` = target_binding.`tenant_id`
     AND target_device.`device_status` = 'ENABLED'
     AND target_device.`enabled` = b'1'
     AND target_device.`deleted` = b'0'
    WHERE project.`tenant_id` = 1
      AND project.`project_code` = 'IDI'
      AND project.`project_name` = '按压式球囊扩充压力泵'
      AND project.`status` = 'ENABLE'
      AND project.`deleted` = 0
      AND NOT EXISTS (
        SELECT 1
        FROM `mes_pro_route` source_route
        JOIN `mes_pro_route_process` source_route_process
          ON source_route_process.`route_id` = source_route.`id`
         AND source_route_process.`tenant_id` = source_route.`tenant_id`
         AND source_route_process.`process_id` = target_route_process.`process_id`
         AND source_route_process.`deleted` = b'0'
        JOIN `mes_pro_process_pool_device_parameter_rule` source_rule
          ON source_rule.`tenant_id` = source_route_process.`tenant_id`
         AND source_rule.`leader_user_id` = target_binding.`leader_user_id`
         AND source_rule.`route_process_id` = source_route_process.`id`
         AND source_rule.`process_id` = target_route_process.`process_id`
         AND source_rule.`device_id` = target_device.`id`
         AND source_rule.`enabled` = b'1'
         AND source_rule.`deleted` = b'0'
        WHERE source_route.`tenant_id` = project.`tenant_id`
          AND source_route.`code` = 'RT000028'
          AND source_route.`deleted` = b'0'
      )
  ) missing_source;

  IF v_missing_source_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target device binding has no source parameter rules';
  END IF;

  SELECT COUNT(*) INTO v_duplicate_source_rule_count
  FROM (
    SELECT
      target_route_process.`tenant_id`,
      target_route_process.`id` AS target_route_process_id,
      target_device.`id` AS target_device_id,
      source_rule.`parameter_code`,
      COUNT(*) AS duplicate_count
    FROM `dcc_project_code` project
    JOIN `mes_pro_route_dcc_project_binding` binding
      ON binding.`dcc_project_code_id` = project.`id`
     AND binding.`tenant_id` = project.`tenant_id`
     AND binding.`deleted` = b'0'
     AND binding.`active_route_id` = binding.`route_id`
    JOIN `mes_pro_route` target_route
      ON target_route.`id` = binding.`route_id`
     AND target_route.`tenant_id` = project.`tenant_id`
     AND target_route.`deleted` = b'0'
     AND target_route.`code` = 'RT000028-IDI'
    JOIN `mes_pro_route_process` target_route_process
      ON target_route_process.`route_id` = target_route.`id`
     AND target_route_process.`tenant_id` = target_route.`tenant_id`
     AND target_route_process.`deleted` = b'0'
    JOIN `mes_pro_process_pool_team_process_device` target_binding
      ON target_binding.`tenant_id` = target_route_process.`tenant_id`
     AND target_binding.`process_id` = target_route_process.`process_id`
     AND target_binding.`enabled` = b'1'
     AND target_binding.`deleted` = b'0'
    JOIN `system_users` leader
      ON leader.`id` = target_binding.`leader_user_id`
     AND leader.`tenant_id` = target_binding.`tenant_id`
     AND leader.`username` = 'admin'
     AND leader.`deleted` = b'0'
    JOIN `mes_pro_process_pool_team_device` target_device
      ON target_device.`id` = target_binding.`device_id`
     AND target_device.`leader_user_id` = target_binding.`leader_user_id`
     AND target_device.`tenant_id` = target_binding.`tenant_id`
     AND target_device.`device_status` = 'ENABLED'
     AND target_device.`enabled` = b'1'
     AND target_device.`deleted` = b'0'
    JOIN `mes_pro_route` source_route
      ON source_route.`tenant_id` = project.`tenant_id`
     AND source_route.`code` = 'RT000028'
     AND source_route.`deleted` = b'0'
    JOIN `mes_pro_route_process` source_route_process
      ON source_route_process.`route_id` = source_route.`id`
     AND source_route_process.`tenant_id` = source_route.`tenant_id`
     AND source_route_process.`process_id` = target_route_process.`process_id`
     AND source_route_process.`deleted` = b'0'
    JOIN `mes_pro_process_pool_device_parameter_rule` source_rule
      ON source_rule.`tenant_id` = source_route_process.`tenant_id`
     AND source_rule.`leader_user_id` = target_binding.`leader_user_id`
     AND source_rule.`route_process_id` = source_route_process.`id`
     AND source_rule.`process_id` = target_route_process.`process_id`
     AND source_rule.`device_id` = target_device.`id`
     AND source_rule.`enabled` = b'1'
     AND source_rule.`deleted` = b'0'
    WHERE project.`tenant_id` = 1
      AND project.`project_code` = 'IDI'
      AND project.`project_name` = '按压式球囊扩充压力泵'
      AND project.`status` = 'ENABLE'
      AND project.`deleted` = 0
    GROUP BY target_route_process.`tenant_id`, target_route_process.`id`, target_device.`id`,
             source_rule.`parameter_code`
    HAVING duplicate_count > 1
  ) duplicate_source;

  IF v_duplicate_source_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI source parameter rules are not unique';
  END IF;

  SELECT COUNT(*) INTO v_conflicting_target_rule_count
  FROM `dcc_project_code` project
  JOIN `mes_pro_route_dcc_project_binding` binding
    ON binding.`dcc_project_code_id` = project.`id`
   AND binding.`tenant_id` = project.`tenant_id`
   AND binding.`deleted` = b'0'
   AND binding.`active_route_id` = binding.`route_id`
  JOIN `mes_pro_route` target_route
    ON target_route.`id` = binding.`route_id`
   AND target_route.`tenant_id` = project.`tenant_id`
   AND target_route.`deleted` = b'0'
   AND target_route.`code` = 'RT000028-IDI'
  JOIN `mes_pro_route_process` target_route_process
    ON target_route_process.`route_id` = target_route.`id`
   AND target_route_process.`tenant_id` = target_route.`tenant_id`
   AND target_route_process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_process_device` target_binding
    ON target_binding.`tenant_id` = target_route_process.`tenant_id`
   AND target_binding.`process_id` = target_route_process.`process_id`
   AND target_binding.`enabled` = b'1'
   AND target_binding.`deleted` = b'0'
  JOIN `system_users` leader
    ON leader.`id` = target_binding.`leader_user_id`
   AND leader.`tenant_id` = target_binding.`tenant_id`
   AND leader.`username` = 'admin'
   AND leader.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` target_device
    ON target_device.`id` = target_binding.`device_id`
   AND target_device.`leader_user_id` = target_binding.`leader_user_id`
   AND target_device.`tenant_id` = target_binding.`tenant_id`
   AND target_device.`device_status` = 'ENABLED'
   AND target_device.`enabled` = b'1'
   AND target_device.`deleted` = b'0'
  JOIN `mes_pro_route` source_route
    ON source_route.`tenant_id` = project.`tenant_id`
   AND source_route.`code` = 'RT000028'
   AND source_route.`deleted` = b'0'
  JOIN `mes_pro_route_process` source_route_process
    ON source_route_process.`route_id` = source_route.`id`
   AND source_route_process.`tenant_id` = source_route.`tenant_id`
   AND source_route_process.`process_id` = target_route_process.`process_id`
   AND source_route_process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_device_parameter_rule` source_rule
    ON source_rule.`tenant_id` = source_route_process.`tenant_id`
   AND source_rule.`leader_user_id` = target_binding.`leader_user_id`
   AND source_rule.`route_process_id` = source_route_process.`id`
   AND source_rule.`process_id` = target_route_process.`process_id`
   AND source_rule.`device_id` = target_device.`id`
   AND source_rule.`enabled` = b'1'
   AND source_rule.`deleted` = b'0'
  JOIN `mes_pro_process_pool_device_parameter_rule` existing_rule
    ON existing_rule.`tenant_id` = target_route_process.`tenant_id`
   AND existing_rule.`route_process_id` = target_route_process.`id`
   AND existing_rule.`process_id` = target_route_process.`process_id`
   AND existing_rule.`device_id` = target_device.`id`
   AND existing_rule.`parameter_code` = source_rule.`parameter_code`
   AND existing_rule.`deleted` = b'0'
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = '按压式球囊扩充压力泵'
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0
    AND NOT (
      existing_rule.`leader_user_id` <=> source_rule.`leader_user_id`
      AND existing_rule.`parameter_name` <=> source_rule.`parameter_name`
      AND existing_rule.`unit` <=> source_rule.`unit`
      AND existing_rule.`lower_limit` <=> source_rule.`lower_limit`
      AND existing_rule.`upper_limit` <=> source_rule.`upper_limit`
      AND existing_rule.`default_value` <=> source_rule.`default_value`
      AND existing_rule.`value_type` <=> source_rule.`value_type`
      AND existing_rule.`standard_text` <=> source_rule.`standard_text`
      AND existing_rule.`option_values_json` <=> source_rule.`option_values_json`
      AND existing_rule.`default_text` <=> source_rule.`default_text`
      AND existing_rule.`decimal_scale` <=> source_rule.`decimal_scale`
      AND existing_rule.`enabled` <=> source_rule.`enabled`
    );

  IF v_conflicting_target_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target parameter rules conflict with source';
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS postflight_mes_pp_idi_device_parameter_rules;
DELIMITER $$
CREATE PROCEDURE postflight_mes_pp_idi_device_parameter_rules()
BEGIN
  DECLARE v_missing_target_rule_count bigint DEFAULT 0;
  DECLARE v_duplicate_target_rule_count bigint DEFAULT 0;

  SELECT COUNT(*) INTO v_missing_target_rule_count
  FROM `dcc_project_code` project
  JOIN `mes_pro_route_dcc_project_binding` binding
    ON binding.`dcc_project_code_id` = project.`id`
   AND binding.`tenant_id` = project.`tenant_id`
   AND binding.`deleted` = b'0'
   AND binding.`active_route_id` = binding.`route_id`
  JOIN `mes_pro_route` target_route
    ON target_route.`id` = binding.`route_id`
   AND target_route.`tenant_id` = project.`tenant_id`
   AND target_route.`deleted` = b'0'
   AND target_route.`code` = 'RT000028-IDI'
  JOIN `mes_pro_route_process` target_route_process
    ON target_route_process.`route_id` = target_route.`id`
   AND target_route_process.`tenant_id` = target_route.`tenant_id`
   AND target_route_process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_process_device` target_binding
    ON target_binding.`tenant_id` = target_route_process.`tenant_id`
   AND target_binding.`process_id` = target_route_process.`process_id`
   AND target_binding.`enabled` = b'1'
   AND target_binding.`deleted` = b'0'
  JOIN `system_users` leader
    ON leader.`id` = target_binding.`leader_user_id`
   AND leader.`tenant_id` = target_binding.`tenant_id`
   AND leader.`username` = 'admin'
   AND leader.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` target_device
    ON target_device.`id` = target_binding.`device_id`
   AND target_device.`leader_user_id` = target_binding.`leader_user_id`
   AND target_device.`tenant_id` = target_binding.`tenant_id`
   AND target_device.`device_status` = 'ENABLED'
   AND target_device.`enabled` = b'1'
   AND target_device.`deleted` = b'0'
  JOIN `mes_pro_route` source_route
    ON source_route.`tenant_id` = project.`tenant_id`
   AND source_route.`code` = 'RT000028'
   AND source_route.`deleted` = b'0'
  JOIN `mes_pro_route_process` source_route_process
    ON source_route_process.`route_id` = source_route.`id`
   AND source_route_process.`tenant_id` = source_route.`tenant_id`
   AND source_route_process.`process_id` = target_route_process.`process_id`
   AND source_route_process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_device_parameter_rule` source_rule
    ON source_rule.`tenant_id` = source_route_process.`tenant_id`
   AND source_rule.`leader_user_id` = target_binding.`leader_user_id`
   AND source_rule.`route_process_id` = source_route_process.`id`
   AND source_rule.`process_id` = target_route_process.`process_id`
   AND source_rule.`device_id` = target_device.`id`
   AND source_rule.`enabled` = b'1'
   AND source_rule.`deleted` = b'0'
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = '按压式球囊扩充压力泵'
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_process_pool_device_parameter_rule` target_rule
      WHERE target_rule.`tenant_id` = target_route_process.`tenant_id`
        AND target_rule.`leader_user_id` = target_binding.`leader_user_id`
        AND target_rule.`route_process_id` = target_route_process.`id`
        AND target_rule.`process_id` = target_route_process.`process_id`
        AND target_rule.`device_id` = target_device.`id`
        AND target_rule.`parameter_code` = source_rule.`parameter_code`
        AND target_rule.`enabled` = b'1'
        AND target_rule.`deleted` = b'0'
    );

  IF v_missing_target_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target parameter rules were not fully inserted';
  END IF;

  SELECT COUNT(*) INTO v_duplicate_target_rule_count
  FROM (
    SELECT
      target_rule.`tenant_id`,
      target_rule.`route_process_id`,
      target_rule.`device_id`,
      target_rule.`parameter_code`,
      COUNT(*) AS duplicate_count
    FROM `dcc_project_code` project
    JOIN `mes_pro_route_dcc_project_binding` binding
      ON binding.`dcc_project_code_id` = project.`id`
     AND binding.`tenant_id` = project.`tenant_id`
     AND binding.`deleted` = b'0'
     AND binding.`active_route_id` = binding.`route_id`
    JOIN `mes_pro_route` target_route
      ON target_route.`id` = binding.`route_id`
     AND target_route.`tenant_id` = project.`tenant_id`
     AND target_route.`deleted` = b'0'
     AND target_route.`code` = 'RT000028-IDI'
    JOIN `mes_pro_route_process` target_route_process
      ON target_route_process.`route_id` = target_route.`id`
     AND target_route_process.`tenant_id` = target_route.`tenant_id`
     AND target_route_process.`deleted` = b'0'
    JOIN `mes_pro_process_pool_device_parameter_rule` target_rule
      ON target_rule.`tenant_id` = target_route_process.`tenant_id`
     AND target_rule.`route_process_id` = target_route_process.`id`
     AND target_rule.`deleted` = b'0'
    WHERE project.`tenant_id` = 1
      AND project.`project_code` = 'IDI'
      AND project.`project_name` = '按压式球囊扩充压力泵'
      AND project.`status` = 'ENABLE'
      AND project.`deleted` = 0
    GROUP BY target_rule.`tenant_id`, target_rule.`route_process_id`, target_rule.`device_id`,
             target_rule.`parameter_code`
    HAVING duplicate_count > 1
  ) duplicate_target;

  IF v_duplicate_target_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target parameter rules are duplicated';
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS migrate_mes_pp_idi_device_parameter_rules;
DELIMITER $$
CREATE PROCEDURE migrate_mes_pp_idi_device_parameter_rules()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  CALL preflight_mes_pp_idi_device_parameter_rules();

  START TRANSACTION;

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
    target_route_process.`id`,
    target_route_process.`process_id`,
    target_device.`id`,
    source_rule.`parameter_code`,
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
    source_rule.`enabled`,
    'IDI DCC路线设备参数正式补齐',
    '20260830_idi_device_parameter_rules',
    NOW(),
    '20260830_idi_device_parameter_rules',
    NOW(),
    b'0',
    target_route_process.`tenant_id`
  FROM `dcc_project_code` project
  JOIN `mes_pro_route_dcc_project_binding` binding
    ON binding.`dcc_project_code_id` = project.`id`
   AND binding.`tenant_id` = project.`tenant_id`
   AND binding.`deleted` = b'0'
   AND binding.`active_route_id` = binding.`route_id`
  JOIN `mes_pro_route` target_route
    ON target_route.`id` = binding.`route_id`
   AND target_route.`tenant_id` = project.`tenant_id`
   AND target_route.`deleted` = b'0'
   AND target_route.`code` = 'RT000028-IDI'
  JOIN `mes_pro_route_process` target_route_process
    ON target_route_process.`route_id` = target_route.`id`
   AND target_route_process.`tenant_id` = target_route.`tenant_id`
   AND target_route_process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_process_device` target_binding
    ON target_binding.`tenant_id` = target_route_process.`tenant_id`
   AND target_binding.`process_id` = target_route_process.`process_id`
   AND target_binding.`enabled` = b'1'
   AND target_binding.`deleted` = b'0'
  JOIN `system_users` leader
    ON leader.`id` = target_binding.`leader_user_id`
   AND leader.`tenant_id` = target_binding.`tenant_id`
   AND leader.`username` = 'admin'
   AND leader.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` target_device
    ON target_device.`id` = target_binding.`device_id`
   AND target_device.`leader_user_id` = target_binding.`leader_user_id`
   AND target_device.`tenant_id` = target_binding.`tenant_id`
   AND target_device.`device_status` = 'ENABLED'
   AND target_device.`enabled` = b'1'
   AND target_device.`deleted` = b'0'
  JOIN `mes_pro_route` source_route
    ON source_route.`tenant_id` = project.`tenant_id`
   AND source_route.`code` = 'RT000028'
   AND source_route.`deleted` = b'0'
  JOIN `mes_pro_route_process` source_route_process
    ON source_route_process.`route_id` = source_route.`id`
   AND source_route_process.`tenant_id` = source_route.`tenant_id`
   AND source_route_process.`process_id` = target_route_process.`process_id`
   AND source_route_process.`deleted` = b'0'
  JOIN `mes_pro_process_pool_device_parameter_rule` source_rule
    ON source_rule.`tenant_id` = source_route_process.`tenant_id`
   AND source_rule.`leader_user_id` = target_binding.`leader_user_id`
   AND source_rule.`route_process_id` = source_route_process.`id`
   AND source_rule.`process_id` = target_route_process.`process_id`
   AND source_rule.`device_id` = target_device.`id`
   AND source_rule.`enabled` = b'1'
   AND source_rule.`deleted` = b'0'
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = '按压式球囊扩充压力泵'
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_process_pool_device_parameter_rule` existing_rule
      WHERE existing_rule.`tenant_id` = target_route_process.`tenant_id`
        AND existing_rule.`route_process_id` = target_route_process.`id`
        AND existing_rule.`process_id` = target_route_process.`process_id`
        AND existing_rule.`device_id` = target_device.`id`
        AND existing_rule.`parameter_code` = source_rule.`parameter_code`
        AND existing_rule.`deleted` = b'0'
    );

  CALL postflight_mes_pp_idi_device_parameter_rules();

  COMMIT;
END$$
DELIMITER ;

CALL migrate_mes_pp_idi_device_parameter_rules();

DROP PROCEDURE IF EXISTS migrate_mes_pp_idi_device_parameter_rules;
DROP PROCEDURE IF EXISTS postflight_mes_pp_idi_device_parameter_rules;
DROP PROCEDURE IF EXISTS preflight_mes_pp_idi_device_parameter_rules;

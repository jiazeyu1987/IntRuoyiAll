-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_process_pool_cleaning_process_parameter_data; type=data; riskLevel=medium
-- 校验 IDI / 按压式球囊扩充压力泵当前 DCC 路线的正式目标设备参数规则。
-- Recovery: this migration is validation-only and performs no business-data INSERT/UPDATE/DELETE.
-- Rollback: no data rollback is required; restore the prior release if the validation contract is rejected.

DROP PROCEDURE IF EXISTS preflight_mes_pp_idi_device_parameter_rules;
DELIMITER $$
CREATE PROCEDURE preflight_mes_pp_idi_device_parameter_rules()
BEGIN
  DECLARE v_project_count bigint DEFAULT 0;
  DECLARE v_target_binding_count bigint DEFAULT 0;
  DECLARE v_source_route_count bigint DEFAULT 0;
  DECLARE v_target_device_count bigint DEFAULT 0;
  DECLARE v_target_rule_count bigint DEFAULT 0;
  DECLARE v_anchor_rule_count bigint DEFAULT 0;
  DECLARE v_duplicate_target_rule_count bigint DEFAULT 0;

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
    AND project.`project_name` = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
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
    AND project.`project_name` = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
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
    AND project.`project_name` = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0;

  IF v_target_device_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target route has no active admin device bindings';
  END IF;

  SELECT COUNT(*) INTO v_target_rule_count
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
  JOIN `mes_pro_process_pool_device_parameter_rule` target_rule
    ON target_rule.`tenant_id` = target_route_process.`tenant_id`
   AND target_rule.`leader_user_id` = target_binding.`leader_user_id`
   AND target_rule.`route_process_id` = target_route_process.`id`
   AND target_rule.`process_id` = target_route_process.`process_id`
   AND target_rule.`device_id` = target_device.`id`
   AND target_rule.`enabled` = b'1'
   AND target_rule.`deleted` = b'0'
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0;

  IF v_target_rule_count < 37 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target route canonical parameter rules are missing';
  END IF;

  SELECT COUNT(DISTINCT target_rule.`parameter_code`) INTO v_anchor_rule_count
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
   AND target_route_process.`sort` = 1
  JOIN `mes_pro_process_pool_team_process_device` target_binding
    ON target_binding.`tenant_id` = target_route_process.`tenant_id`
   AND target_binding.`process_id` = target_route_process.`process_id`
   AND target_binding.`enabled` = b'1'
   AND target_binding.`deleted` = b'0'
  JOIN `mes_pro_process_pool_team_device` target_device
    ON target_device.`id` = target_binding.`device_id`
   AND target_device.`device_code` = 'B09393'
   AND target_device.`leader_user_id` = target_binding.`leader_user_id`
   AND target_device.`tenant_id` = target_binding.`tenant_id`
   AND target_device.`device_status` = 'ENABLED'
   AND target_device.`enabled` = b'1'
   AND target_device.`deleted` = b'0'
  JOIN `mes_pro_process_pool_device_parameter_rule` target_rule
    ON target_rule.`tenant_id` = target_route_process.`tenant_id`
   AND target_rule.`leader_user_id` = target_binding.`leader_user_id`
   AND target_rule.`route_process_id` = target_route_process.`id`
   AND target_rule.`process_id` = target_route_process.`process_id`
   AND target_rule.`device_id` = target_device.`id`
   AND target_rule.`enabled` = b'1'
   AND target_rule.`deleted` = b'0'
   AND target_rule.`parameter_code` IN (
     'IDIJSON_01_B09393_01',
     'IDIJSON_01_B09393_02',
     'IDIJSON_01_B09393_03',
     'IDIJSON_01_B09393_04',
     'IDIJSON_01_B09393_05'
   )
  WHERE project.`tenant_id` = 1
    AND project.`project_code` = 'IDI'
    AND project.`project_name` = _utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5
    AND project.`status` = 'ENABLE'
    AND project.`deleted` = 0;

  IF v_anchor_rule_count <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target route canonical parameter anchor is missing';
  END IF;

  SELECT COUNT(*) INTO v_duplicate_target_rule_count
  FROM (
    SELECT
      target_rule.`tenant_id`,
      target_rule.`route_process_id`,
      target_rule.`device_id`,
      target_rule.`parameter_code`,
      COUNT(*) AS duplicate_count
    FROM `mes_pro_process_pool_device_parameter_rule` target_rule
    JOIN `mes_pro_route_process` target_route_process
      ON target_route_process.`id` = target_rule.`route_process_id`
     AND target_route_process.`tenant_id` = target_rule.`tenant_id`
     AND target_route_process.`deleted` = b'0'
    JOIN `mes_pro_route` target_route
      ON target_route.`id` = target_route_process.`route_id`
     AND target_route.`tenant_id` = target_route_process.`tenant_id`
     AND target_route.`code` = 'RT000028-IDI'
     AND target_route.`deleted` = b'0'
    WHERE target_rule.`tenant_id` = 1
      AND target_rule.`enabled` = b'1'
      AND target_rule.`deleted` = b'0'
    GROUP BY target_rule.`tenant_id`, target_rule.`route_process_id`,
             target_rule.`device_id`, target_rule.`parameter_code`
    HAVING duplicate_count > 1
  ) duplicate_target;

  IF v_duplicate_target_rule_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'IDI target route canonical parameter rules are duplicated';
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS postflight_mes_pp_idi_device_parameter_rules;
DELIMITER $$
CREATE PROCEDURE postflight_mes_pp_idi_device_parameter_rules()
BEGIN
  CALL preflight_mes_pp_idi_device_parameter_rules();
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
  CALL postflight_mes_pp_idi_device_parameter_rules();
  COMMIT;
END$$
DELIMITER ;

CALL migrate_mes_pp_idi_device_parameter_rules();

DROP PROCEDURE IF EXISTS migrate_mes_pp_idi_device_parameter_rules;
DROP PROCEDURE IF EXISTS postflight_mes_pp_idi_device_parameter_rules;
DROP PROCEDURE IF EXISTS preflight_mes_pp_idi_device_parameter_rules;

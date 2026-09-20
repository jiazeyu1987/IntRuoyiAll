-- release-target-preflight: migrationId=20260811_mes_process_pool_uv2_two_device_runtime_config; allowedEnvironments=test,backup,prod
WITH
schema_tables AS (
  SELECT COUNT(*) AS table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND LOWER(table_name) IN (
      'mes_pro_process',
      'mes_pro_process_pool_device_parameter_rule',
      'mes_pro_process_pool_team_device',
      'mes_pro_process_pool_team_process_device'
    )
),
source_bindings AS (
  SELECT COUNT(*) AS source_binding_count
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
    AND source_device.`device_status` = 'ENABLED'
),
missing_target_devices AS (
  SELECT COUNT(*) AS missing_target_device_count
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
    )
),
missing_source_rules AS (
  SELECT COUNT(*) AS missing_source_rule_count
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
    )
),
conflicting_metering_valid AS (
  SELECT COUNT(*) AS conflicting_metering_valid_count
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
)
SELECT CASE WHEN
  (SELECT table_count FROM schema_tables) = 4
  AND (SELECT source_binding_count FROM source_bindings) >= 0
  AND (SELECT missing_target_device_count FROM missing_target_devices) = 0
  AND (SELECT missing_source_rule_count FROM missing_source_rules) = 0
  AND (SELECT conflicting_metering_valid_count FROM conflicting_metering_valid) = 0
THEN 'TARGET_PREFLIGHT_PASS:20260811_mes_process_pool_uv2_two_device_runtime_config' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260811_mes_process_pool_uv2_two_device_runtime_config' END;

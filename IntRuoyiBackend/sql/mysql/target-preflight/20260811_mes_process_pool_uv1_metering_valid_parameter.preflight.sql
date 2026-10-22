-- release-target-preflight: migrationId=20260811_mes_process_pool_uv1_metering_valid_parameter; allowedEnvironments=test,backup,prod
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
schema_columns AS (
  SELECT COUNT(*) AS column_count
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
),
source_scopes AS (
  SELECT COUNT(DISTINCT
    source_rule.`tenant_id`,
    source_rule.`leader_user_id`,
    source_rule.`route_process_id`,
    source_rule.`process_id`
  ) AS source_scope_count
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
    )
),
legacy_metering AS (
  SELECT COUNT(*) AS legacy_metering_count
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
),
conflicting_a05059_device AS (
  SELECT COUNT(*) AS conflicting_a05059_device_count
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
),
disabled_a05059_binding AS (
  SELECT COUNT(*) AS disabled_a05059_binding_count
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
),
conflicting_a05059_rule AS (
  SELECT COUNT(*) AS conflicting_a05059_rule_count
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
   AND target_rule.`parameter_code` = CASE
     WHEN source_rule.`parameter_code` LIKE '%A05075%'
     THEN CONCAT(
       SUBSTRING_INDEX(source_rule.`parameter_code`, 'A05075', 1),
       'A05059',
       SUBSTRING_INDEX(source_rule.`parameter_code`, 'A05075', -1)
     )
     ELSE source_rule.`parameter_code`
   END
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
),
conflicting_metering_valid AS (
  SELECT COUNT(*) AS conflicting_metering_valid_count
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
)
SELECT CASE WHEN
  (SELECT table_count FROM schema_tables) = 4
  AND (SELECT column_count FROM schema_columns) = 7
  AND (SELECT source_scope_count FROM source_scopes) >= 0
  AND (SELECT legacy_metering_count FROM legacy_metering) = 0
  AND (SELECT conflicting_a05059_device_count FROM conflicting_a05059_device) = 0
  AND (SELECT disabled_a05059_binding_count FROM disabled_a05059_binding) = 0
  AND (SELECT conflicting_a05059_rule_count FROM conflicting_a05059_rule) = 0
  AND (SELECT conflicting_metering_valid_count FROM conflicting_metering_valid) = 0
THEN 'TARGET_PREFLIGHT_PASS:20260811_mes_process_pool_uv1_metering_valid_parameter' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260811_mes_process_pool_uv1_metering_valid_parameter' END;

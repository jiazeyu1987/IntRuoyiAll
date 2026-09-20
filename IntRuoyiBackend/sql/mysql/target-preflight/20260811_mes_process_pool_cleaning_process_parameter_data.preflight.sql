-- release-target-preflight: migrationId=20260811_mes_process_pool_cleaning_process_parameter_data; allowedEnvironments=test,backup,prod
WITH dependency_tables AS (
  SELECT COUNT(*) AS dependency_table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND LOWER(table_name) IN (
      'mes_pro_process',
      'mes_pro_route_process',
      'mes_pro_process_pool_device_parameter_rule',
      'mes_pro_process_pool_team_device'
    )
),
typed_columns AS (
  SELECT COUNT(*) AS typed_column_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND LOWER(table_name) = 'mes_pro_process_pool_device_parameter_rule'
    AND LOWER(column_name) IN ('option_values_json', 'default_text', 'decimal_scale', 'standard_text')
),
candidate_rules AS (
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
),
candidate_summary AS (
  SELECT COUNT(*) AS candidate_rule_count
  FROM candidate_rules
  WHERE target_parameter_code IS NOT NULL
),
duplicate_summary AS (
  SELECT COUNT(*) AS duplicate_target_count
  FROM (
    SELECT tenant_id, route_process_id, device_id, target_parameter_code
    FROM candidate_rules
    WHERE target_parameter_code IS NOT NULL
    GROUP BY tenant_id, route_process_id, device_id, target_parameter_code
    HAVING COUNT(*) > 1
  ) duplicate_targets
),
incomplete_summary AS (
  SELECT COUNT(*) AS incomplete_target_count
  FROM (
    SELECT tenant_id, route_process_id, device_id, COUNT(DISTINCT target_parameter_code) AS formal_parameter_count
    FROM candidate_rules
    WHERE target_parameter_code IS NOT NULL
    GROUP BY tenant_id, route_process_id, device_id
    HAVING formal_parameter_count <> 5
  ) incomplete_targets
)
SELECT CASE WHEN
  dependency_tables.dependency_table_count = 4
  AND typed_columns.typed_column_count = 4
  AND (
    candidate_summary.candidate_rule_count = 0
    OR (
      duplicate_summary.duplicate_target_count = 0
      AND incomplete_summary.incomplete_target_count = 0
    )
  )
THEN 'TARGET_PREFLIGHT_PASS:20260811_mes_process_pool_cleaning_process_parameter_data' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260811_mes_process_pool_cleaning_process_parameter_data' END
FROM dependency_tables
CROSS JOIN typed_columns
CROSS JOIN candidate_summary
CROSS JOIN duplicate_summary
CROSS JOIN incomplete_summary;

-- release-target-preflight: migrationId=20260812_mes_process_pool_b04091_cleaning_temperature_label; allowedEnvironments=test,backup,prod
WITH
dependency_tables AS (
  SELECT COUNT(*) AS table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND LOWER(table_name) IN ('mes_pro_process','mes_pro_process_pool_device_parameter_rule','mes_pro_process_pool_team_device')
),
candidate_rules AS (
  SELECT COUNT(*) AS candidate_rule_count
  FROM mes_pro_process_pool_device_parameter_rule rule
  JOIN mes_pro_process_pool_team_device device
    ON device.id = rule.device_id
   AND device.tenant_id = rule.tenant_id
   AND device.deleted = b'0'
  JOIN mes_pro_process process
    ON process.id = rule.process_id
   AND process.tenant_id = rule.tenant_id
   AND process.deleted = b'0'
  WHERE rule.deleted = b'0'
    AND process.name = '清洗工序'
    AND device.device_code = 'B04091'
    AND rule.parameter_code = 'CLEANING_ROOM_TEMPERATURE'
    AND rule.parameter_name IN ('室温', '清洗温度')
)
SELECT CASE WHEN
  (SELECT table_count FROM dependency_tables) = 3
  AND (SELECT candidate_rule_count FROM candidate_rules) >= 0
THEN 'TARGET_PREFLIGHT_PASS:20260812_mes_process_pool_b04091_cleaning_temperature_label' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260812_mes_process_pool_b04091_cleaning_temperature_label' END;

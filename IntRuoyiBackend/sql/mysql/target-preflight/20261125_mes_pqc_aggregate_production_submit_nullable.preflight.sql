-- release-target-preflight: migrationId=20261125_mes_pqc_aggregate_production_submit_nullable; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_pqc_record'
      AND COLUMN_NAME='production_submit_event_id' AND COLUMN_TYPE='bigint'
      AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL) = 1
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_process_inspection_aggregate_detail'
      AND COLUMN_NAME='production_submit_event_id' AND COLUMN_TYPE='bigint'
      AND COLUMN_DEFAULT IS NULL AND EXTRA='') = 1
THEN 'TARGET_PREFLIGHT_PASS:20261125_mes_pqc_aggregate_production_submit_nullable'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20261125_mes_pqc_aggregate_production_submit_nullable' END;

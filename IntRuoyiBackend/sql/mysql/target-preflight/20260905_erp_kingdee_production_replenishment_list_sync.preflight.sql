-- release-target-preflight: migrationId=20260905_erp_kingdee_production_replenishment_list_sync; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role_menu','infra_job')) = 3
  AND (SELECT COUNT(*) FROM system_menu WHERE path = 'production' AND deleted = b'0') = 1
  AND (SELECT COUNT(*) FROM system_menu WHERE path = 'replenishment-list' AND deleted = b'0') <= 1
THEN 'TARGET_PREFLIGHT_PASS:20260905_erp_kingdee_production_replenishment_list_sync' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260905_erp_kingdee_production_replenishment_list_sync' END;

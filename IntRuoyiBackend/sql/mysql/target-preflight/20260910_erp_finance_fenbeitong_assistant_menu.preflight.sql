-- release-target-preflight: migrationId=20260910_erp_finance_fenbeitong_assistant_menu; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu')) = 1
  AND (SELECT COUNT(*) FROM system_menu WHERE id = 2645 AND deleted = b'0') = 1
  AND (SELECT COUNT(*) FROM system_menu WHERE id = 991200 AND deleted = b'0') <= 1
THEN 'TARGET_PREFLIGHT_PASS:20260910_erp_finance_fenbeitong_assistant_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260910_erp_finance_fenbeitong_assistant_menu' END;

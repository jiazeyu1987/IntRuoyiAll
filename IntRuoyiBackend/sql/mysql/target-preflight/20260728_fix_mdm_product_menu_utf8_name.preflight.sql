-- release-target-preflight: migrationId=20260728_fix_mdm_product_menu_utf8_name; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu')) = 1
  AND (SELECT COUNT(*) FROM system_menu WHERE id = 990201 AND deleted = b'0') <= 1
THEN 'TARGET_PREFLIGHT_PASS:20260728_fix_mdm_product_menu_utf8_name' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260728_fix_mdm_product_menu_utf8_name' END;

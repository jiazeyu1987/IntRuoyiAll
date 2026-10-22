-- release-target-preflight: migrationId=20260729_dcc_product_catalog_remove_subsidiary_source; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_product_catalog')) = 1
THEN 'TARGET_PREFLIGHT_PASS:20260729_dcc_product_catalog_remove_subsidiary_source' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260729_dcc_product_catalog_remove_subsidiary_source' END;

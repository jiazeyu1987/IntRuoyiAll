-- release-target-preflight: migrationId=20261117_erp_product_tenant_bar_code_index; allowedEnvironments=test,backup,prod
SELECT CASE
 WHEN NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_product') THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_erp_product_tenant_bar_code_index:TABLE_MISSING'
 WHEN EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_product' AND INDEX_NAME='idx_erp_product_tenant_bar_deleted') THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_erp_product_tenant_bar_code_index:INDEX_ALREADY_EXISTS'
 ELSE 'TARGET_PREFLIGHT_PASS:20261117_erp_product_tenant_bar_code_index' END;

-- release-target-preflight: migrationId=20261118_erp_sale_order_no_index; allowedEnvironments=test,backup,prod
SELECT CASE
 WHEN NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_sale_order') THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_erp_sale_order_no_index:TABLE_MISSING'
 WHEN EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_sale_order' AND INDEX_NAME='idx_erp_sale_tenant_no_deleted') THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_erp_sale_order_no_index:INDEX_ALREADY_EXISTS'
 ELSE 'TARGET_PREFLIGHT_PASS:20261118_erp_sale_order_no_index' END;

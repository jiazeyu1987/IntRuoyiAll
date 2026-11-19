-- release-target-preflight: migrationId=20261118_erp_sale_order_item_order_index; allowedEnvironments=test,backup,prod
SELECT CASE
 WHEN NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_sale_order_items') THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_erp_sale_order_item_order_index:TABLE_MISSING'
 WHEN EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_sale_order_items' AND INDEX_NAME='idx_erp_sale_items_tenant_order_deleted') THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_erp_sale_order_item_order_index:INDEX_ALREADY_EXISTS'
 ELSE 'TARGET_PREFLIGHT_PASS:20261118_erp_sale_order_item_order_index' END;

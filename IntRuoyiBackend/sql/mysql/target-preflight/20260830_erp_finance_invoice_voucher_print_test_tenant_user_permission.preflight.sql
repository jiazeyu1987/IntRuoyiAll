-- release-target-preflight: migrationId=20260830_erp_finance_invoice_voucher_print_test_tenant_user_permission; allowedEnvironments=test
-- Exact existing menu/role/user identities; never creates substitute accounts or grants.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_tenant WHERE id=122 AND name=CONVERT(UNHEX('E6B58BE8AF95E7A79FE688B7') USING utf8mb4) COLLATE utf8mb4_unicode_ci AND deleted=b'0')
 AND (SELECT COUNT(*) FROM system_users WHERE username='aoteman' AND tenant_id=122 AND status=0 AND deleted=b'0')=1
 AND (SELECT COUNT(*) FROM system_role WHERE code='finance_invoice_voucher_print' AND tenant_id=122 AND status=0 AND deleted=b'0')=1
 THEN 'TARGET_PREFLIGHT_PASS:20260830_erp_finance_invoice_voucher_print_test_tenant_user_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260830_erp_finance_invoice_voucher_print_test_tenant_user_permission:MISSING_AMBIGUOUS_OR_CROSS_TENANT_IDENTITY' END;

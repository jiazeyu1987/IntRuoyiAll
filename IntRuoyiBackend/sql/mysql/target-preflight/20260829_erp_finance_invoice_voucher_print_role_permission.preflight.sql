-- release-target-preflight: migrationId=20260829_erp_finance_invoice_voucher_print_role_permission; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
WITH package_scope AS(SELECT p.id FROM system_tenant_package p WHERE p.deleted=0 AND CASE WHEN JSON_VALID(p.menu_ids) THEN
 JSON_CONTAINS(p.menu_ids,CAST('2563' AS JSON),'$') OR JSON_CONTAINS(p.menu_ids,CAST('2645' AS JSON),'$') OR JSON_CONTAINS(p.menu_ids,CAST('6034' AS JSON),'$') ELSE FALSE END
 UNION SELECT p.id FROM system_tenant t JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 WHERE t.id=122 AND t.name='测试租户' AND t.deleted=0),
 target AS(SELECT t.id tenant_id FROM system_tenant t JOIN package_scope p ON p.id=t.package_id WHERE t.deleted=0
 UNION SELECT id FROM system_tenant WHERE id=1 AND deleted=0
 UNION SELECT r.tenant_id FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id AND r.deleted=0 JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 WHERE rm.menu_id=6034 AND rm.deleted=0)
SELECT CASE WHEN
  (NOT EXISTS(SELECT 1 FROM system_tenant_package p WHERE p.deleted=0 AND
 CASE WHEN p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids) THEN TRUE
 WHEN JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY' THEN TRUE
 ELSE EXISTS(SELECT 1 FROM JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(n FOR ORDINALITY)) j
 WHERE JSON_TYPE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']')))<>'INTEGER'
 OR CAST(JSON_UNQUOTE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']'))) AS DECIMAL(65,0)) NOT BETWEEN 0 AND 9223372036854775807) END))
  AND ((SELECT COUNT(*) FROM system_menu WHERE id IN(2563,2645) AND deleted=0 AND status=0)=2)
  AND (EXISTS(SELECT 1 FROM system_menu m WHERE m.id=6034 AND m.deleted=0 AND m.status=0 AND m.type=2 AND m.parent_id=2645 AND BINARY m.permission=BINARY 'erp:invoice-voucher-print:query' AND BINARY m.path=BINARY 'invoice-voucher-print' AND BINARY m.component=BINARY 'erp/finance/invoice-voucher-print/index' AND BINARY m.component_name=BINARY 'ErpInvoiceVoucherPrint' AND BINARY m.name=BINARY '发票凭证打印'))
  AND (EXISTS(SELECT 1 FROM package_scope))
  AND (EXISTS(SELECT 1 FROM target))
  AND ((SELECT COUNT(*) FROM system_users WHERE tenant_id=1 AND username='admin' AND status=0 AND deleted=0)=1)
  AND (NOT EXISTS(SELECT 1 FROM system_role_category c JOIN target t ON t.tenant_id=c.tenant_id WHERE c.code='finance' GROUP BY c.tenant_id HAVING COUNT(*)>1))
  AND (NOT EXISTS(SELECT 1 FROM system_role r JOIN target t ON t.tenant_id=r.tenant_id WHERE r.code='finance_invoice_voucher_print' GROUP BY r.tenant_id HAVING COUNT(*)>1))
  AND (NOT EXISTS(SELECT 1 FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id WHERE rm.menu_id=6034 AND rm.deleted=0 AND rm.tenant_id<>r.tenant_id))
THEN 'TARGET_PREFLIGHT_PASS:20260829_erp_finance_invoice_voucher_print_role_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260829_erp_finance_invoice_voucher_print_role_permission' END;

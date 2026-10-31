-- release-target-preflight: migrationId=20260828_erp_finance_invoice_voucher_print_menu; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
SELECT CASE WHEN
  (EXISTS(SELECT 1 FROM system_menu WHERE id=2645 AND deleted=0 AND status=0 AND type=1))
  AND (NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=6034 AND NOT COALESCE(m.id=6034 AND m.deleted=0 AND m.status=0 AND m.type=2 AND m.parent_id=2645 AND BINARY m.permission=BINARY 'erp:invoice-voucher-print:query' AND BINARY m.path=BINARY 'invoice-voucher-print' AND BINARY m.component=BINARY 'erp/finance/invoice-voucher-print/index' AND BINARY m.component_name=BINARY 'ErpInvoiceVoucherPrint',FALSE)))
  AND (NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.deleted=0 AND m.id<>6034 AND (m.permission='erp:invoice-voucher-print:query' OR (m.parent_id=2645 AND m.path='invoice-voucher-print') OR m.component_name='ErpInvoiceVoucherPrint')))
THEN 'TARGET_PREFLIGHT_PASS:20260828_erp_finance_invoice_voucher_print_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260828_erp_finance_invoice_voucher_print_menu' END;

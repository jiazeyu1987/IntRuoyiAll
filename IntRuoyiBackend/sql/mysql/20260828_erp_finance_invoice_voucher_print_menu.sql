-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- ERP 系统 / 财务管理 / 发票凭证打印

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    6034, '发票凭证打印', 'erp:invoice-voucher-print:query', 2, 90, 2645, 'invoice-voucher-print', 'ep:printer',
    'erp/finance/invoice-voucher-print/index', 'ErpInvoiceVoucherPrint', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
) ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `type` = VALUES(`type`),
    `sort` = VALUES(`sort`),
    `parent_id` = VALUES(`parent_id`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`),
    `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, menu_ids.`menu_id`, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `system_role_menu` rm
JOIN (
    SELECT 6034 AS `menu_id`
) menu_ids
WHERE rm.`menu_id` = 2645
  AND rm.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` exists_rm
      WHERE exists_rm.`role_id` = rm.`role_id`
        AND exists_rm.`menu_id` = menu_ids.`menu_id`
        AND exists_rm.`tenant_id` = rm.`tenant_id`
        AND exists_rm.`deleted` = b'0'
  );

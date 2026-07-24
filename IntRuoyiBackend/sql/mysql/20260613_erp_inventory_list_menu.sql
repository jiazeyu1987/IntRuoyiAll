-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- ERP 系统 / 生产管理 / 即时库存

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    6025, '即时库存', '', 2, 30, 6020, 'inventory-list', 'ep:box', 'erp/production/inventory-list/index', 'ErpInventoryList',
    0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
) ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
    `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
    `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    6026, '即时库存查询', 'erp:inventory-list:query', 3, 1, 6025, '', '', '', '',
    0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
) ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
    `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
    `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, menu_ids.`menu_id`, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `system_role_menu` rm
JOIN (
    SELECT 6025 AS `menu_id`
    UNION ALL SELECT 6026
) menu_ids
WHERE rm.`menu_id` = 6020
  AND rm.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM `system_role_menu` exists_rm
      WHERE exists_rm.`role_id` = rm.`role_id`
        AND exists_rm.`menu_id` = menu_ids.`menu_id`
        AND exists_rm.`tenant_id` = rm.`tenant_id`
        AND exists_rm.`deleted` = b'0'
  );

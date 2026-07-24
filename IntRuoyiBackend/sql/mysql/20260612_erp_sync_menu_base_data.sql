-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Move ERP/Kingdee sync-related page menus under MES > 基础数据.
-- This migration only updates page menus. Button permissions and role bindings keep their existing menu IDs.

UPDATE `system_menu`
SET `parent_id` = 5101,
    `path` = 'erp-product',
    `sort` = 20,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2565
  AND `type` = 2
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 5101,
    `path` = 'erp-stock',
    `sort` = 21,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2590
  AND `type` = 2
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 5101,
    `path` = 'erp-purchase-order',
    `sort` = 22,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2666
  AND `type` = 2
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 5101,
    `path` = 'erp-sale-order',
    `sort` = 23,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2638
  AND `type` = 2
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 5101,
    `path` = 'work-order',
    `sort` = 24,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 5530
  AND `type` = 2
  AND `deleted` = b'0';

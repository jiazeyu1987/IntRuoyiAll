-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260728_rename_mdm_product_menu,20260626_dcc_basic_data_global_submenu; type=menu; riskLevel=low
-- Move DCC project-code and showroom master-data pages under MES > 基础数据.
-- Existing menu IDs, permissions, components, child button permissions, and role bindings are preserved.

START TRANSACTION;

UPDATE `system_menu`
SET `parent_id` = 5101,
    `path` = 'dcc-project-code',
    `sort` = 30,
    `updater` = 'mes-basic-data-menu',
    `update_time` = NOW()
WHERE `id` = 990210
  AND `type` = 2
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 5101,
    `path` = 'showroom-product',
    `sort` = 31,
    `updater` = 'mes-basic-data-menu',
    `update_time` = NOW()
WHERE `id` = 990201
  AND `type` = 2
  AND `deleted` = b'0';

COMMIT;

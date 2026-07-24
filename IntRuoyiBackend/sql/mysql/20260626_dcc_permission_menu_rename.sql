-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=data; riskLevel=low
-- 将 DCC 文件类别 菜单统一改名为 文控权限

BEGIN;

UPDATE `system_menu`
SET `name` = '文控权限',
    `updater` = 'dcc-permission-rename',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
    `id` = 6803
    OR `path` = 'controlled-file/categories'
  );

COMMIT;

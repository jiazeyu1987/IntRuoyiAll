-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=data; riskLevel=low
-- 退役独立 DCC访问规则 菜单入口，保留历史菜单记录与旧路由兼容壳层

BEGIN;

UPDATE `system_menu`
SET `visible` = b'0',
    `status` = 1,
    `updater` = 'dcc-access-rule-retire',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`id` = 6802 OR `path` = 'controlled-file/access-rules');

COMMIT;

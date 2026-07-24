-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260515_dcc_governance_split_menu; type=data; riskLevel=low
-- 退役独立 文件分发/培训规则 菜单入口，保留历史菜单记录与旧路由兼容壳层

BEGIN;

UPDATE `system_menu`
SET `visible` = b'0',
    `status` = 1,
    `updater` = 'dcc-distribution-training-retire',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
    `id` IN (6808, 6809)
    OR `path` IN ('controlled-file/distribution', 'controlled-file/training')
  );

COMMIT;

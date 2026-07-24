-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=menu; riskLevel=low
-- Rename the DCC controlled-file route tab to 上传审批.

SET NAMES utf8mb4;

UPDATE `system_menu`
SET `name` = '上传审批',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
      `id` = 6805
      OR `path` = 'controlled-file/routes'
      OR `permission` = 'dcc:controlled-file:route:manage'
  );

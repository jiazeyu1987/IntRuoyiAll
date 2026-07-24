-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=menu; riskLevel=low
-- Rename the DCC upload and controlled-browser menu labels.

SET NAMES utf8mb4;

UPDATE `system_menu`
SET `name` = '文件上传',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
      `id` = 6806
      OR `path` = 'controlled-file/upload'
      OR `permission` = 'dcc:controlled-file:submit'
  );

UPDATE `system_menu`
SET `name` = '受控浏览',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
      `id` = 6807
      OR `path` = 'controlled-file/browser'
      OR `permission` = 'dcc:controlled-file:query'
  );

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_dcc_route_upload_approval_rename,20260714_dcc_distribution_training_menu_retire; type=menu; riskLevel=low
-- Hide retired DCC sidebar entries while preserving hidden-route compatibility.
-- 文控管理员 stays visible in menu data for admin and is filtered in AuthController for non-admin users.

SET NAMES utf8mb4;

UPDATE `system_menu`
SET `name` = '上传审批',
    `visible` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
      `id` = 6805
      OR `path` = 'controlled-file/routes'
      OR `permission` = 'dcc:controlled-file:route:manage'
  );

UPDATE `system_menu`
SET `visible` = b'0',
    `always_show` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `path` IN (
      'controlled-file/distribution',
      'controlled-file/training',
      'controlled-file/training-mine',
      'controlled-file/print-template'
  );

UPDATE `system_menu`
SET `visible` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `path` = 'controlled-file/admin';

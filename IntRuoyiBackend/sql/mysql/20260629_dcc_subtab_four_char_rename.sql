-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Rename DCC control-center subtabs to unique four-character names without the DCC prefix.

UPDATE `system_menu`
SET `name` = '文档目录',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/directories'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '上传审批',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/routes'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '文件分发',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/distribution'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '培训规则',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/training'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '文件上传',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/upload'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '受控浏览',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/browser'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '文件审计',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/audit'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '我的培训',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/training-mine'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '模板配置',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/print-template'
  AND `deleted` = b'0';

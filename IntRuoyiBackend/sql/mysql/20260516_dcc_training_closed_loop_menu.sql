-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的培训', 'dcc:controlled-file:training:mine', 2, 13, 6800, 'controlled-file/training-mine', 'ep:reading', 'dcc/controlled-file/training/mine/index', 'DccControlledFileTrainingMine', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path` = 'controlled-file/training-mine');

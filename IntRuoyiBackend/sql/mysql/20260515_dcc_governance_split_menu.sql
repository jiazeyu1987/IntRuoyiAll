-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- DCC governance split menu patch.
-- Adds dedicated DCC distribution/training tabs and shifts later sibling sort values.

UPDATE `system_menu`
SET `sort` = 8, `update_time` = NOW(), `updater` = '1'
WHERE `id` = 6806 AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 9, `update_time` = NOW(), `updater` = '1'
WHERE `id` = 6807 AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 10, `update_time` = NOW(), `updater` = '1'
WHERE `id` = 6813 AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 11, `update_time` = NOW(), `updater` = '1'
WHERE `id` = 6814 AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6808, '文件分发', 'dcc:controlled-file:category:manage', 2, 6, 6800, 'controlled-file/distribution', 'ep:share', 'dcc/controlled-file/distribution/index', 'DccControlledFileDistribution', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6808 OR `path` = 'controlled-file/distribution');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6809, '培训规则', 'dcc:controlled-file:category:manage', 2, 7, 6800, 'controlled-file/training', 'ep:reading', 'dcc/controlled-file/training/index', 'DccControlledFileTraining', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6809 OR `path` = 'controlled-file/training');

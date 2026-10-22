-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=seed; riskLevel=medium
-- Split DCC download authorization by artifact version.

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
 `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6831, 'DCC不可编辑版本下载', 'dcc:controlled-file:download-read-only', 3, 2, 6807, '', '',
       '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'dcc:controlled-file:download-read-only'
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
 `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6832, 'DCC可编辑版本下载', 'dcc:controlled-file:download-editable', 3, 3, 6807, '', '',
       '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'dcc:controlled-file:download-editable'
);

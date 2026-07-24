-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- ERP Kingdee sync runtime page menu seed.

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6013, '金蝶同步运行', '', 2, 91, 2563, 'kingdee-sync', 'ep:operation', 'erp/sync/index', 'ErpKingdeeSync', 0, b'1', b'1', b'1', '', NOW(), '', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6013);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6014, '同步运行查询', 'erp:kingdee-sync:query', 3, 1, 6013, '', '', '', NULL, 0, b'1', b'1', b'1', '', NOW(), '', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6014);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.role_id, 6013, '', NOW(), '', NOW(), b'0', rm.tenant_id
FROM `system_role_menu` rm
WHERE rm.menu_id = 2563
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` ex WHERE ex.role_id = rm.role_id AND ex.menu_id = 6013 AND ex.deleted = b'0'
  );

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.role_id, 6014, '', NOW(), '', NOW(), b'0', rm.tenant_id
FROM `system_role_menu` rm
WHERE rm.menu_id = 2563
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` ex WHERE ex.role_id = rm.role_id AND ex.menu_id = 6014 AND ex.deleted = b'0'
  );

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 6013),
    '$', 6014
)
WHERE JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('2563' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('6013' AS JSON), '$');

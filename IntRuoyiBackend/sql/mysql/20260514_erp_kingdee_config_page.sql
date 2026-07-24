-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=medium
-- ERP Kingdee config page menu seed
-- Idempotent inserts for the ERP configuration management page and its permissions.

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6010, '配置管理', '', 2, 90, 2563, 'kingdee-config', 'ep:setting', 'erp/config/index', 'ErpKingdeeConfig', 0, b'1', b'1', b'1', '', NOW(), '', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6010);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6011, '配置查询', 'erp:kingdee-config:query', 3, 1, 6010, '', '', '', NULL, 0, b'1', b'1', b'1', '', NOW(), '', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6011);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6012, '配置保存', 'erp:kingdee-config:save', 3, 2, 6010, '', '', '', NULL, 0, b'1', b'1', b'1', '', NOW(), '', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6012);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.role_id, 6010, '', NOW(), '', NOW(), b'0', rm.tenant_id
FROM `system_role_menu` rm
WHERE rm.menu_id = 2563
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` ex WHERE ex.role_id = rm.role_id AND ex.menu_id = 6010 AND ex.deleted = b'0'
  );

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.role_id, 6011, '', NOW(), '', NOW(), b'0', rm.tenant_id
FROM `system_role_menu` rm
WHERE rm.menu_id = 2563
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` ex WHERE ex.role_id = rm.role_id AND ex.menu_id = 6011 AND ex.deleted = b'0'
  );

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.role_id, 6012, '', NOW(), '', NOW(), b'0', rm.tenant_id
FROM `system_role_menu` rm
WHERE rm.menu_id = 2563
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` ex WHERE ex.role_id = rm.role_id AND ex.menu_id = 6012 AND ex.deleted = b'0'
  );

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
        JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 6010),
        '$', 6011
    ),
    '$', 6012
)
WHERE JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('2563' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('6010' AS JSON), '$');

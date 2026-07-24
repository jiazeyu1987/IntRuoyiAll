-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES production work order: create Kingdee PRD_MO production order permission and duplicate protection.
-- This script is idempotent. It intentionally fails if existing data violates the new unique work-order link rule.

SET @mes_work_order_create_erp_menu_id := 900200;

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT @mes_work_order_create_erp_menu_id, '创建ERP订单', 'mes:pro-work-order:create-erp',
       3, 6, 5530, '', '', '', NULL, 0, b'1', b'1', b'1', '', NOW(), '', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'mes:pro-work-order:create-erp'
       OR `id` = @mes_work_order_create_erp_menu_id
);

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', @mes_work_order_create_erp_menu_id)
WHERE JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5530' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST(@mes_work_order_create_erp_menu_id AS JSON), '$');

UPDATE `system_tenant_package` AS `package`
INNER JOIN `system_tenant` AS `tenant`
        ON `tenant`.`package_id` = `package`.`id`
       AND `tenant`.`name` = '测试租户'
       AND `tenant`.`deleted` = b'0'
SET `package`.`menu_ids` = JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', @mes_work_order_create_erp_menu_id)
WHERE JSON_VALID(`package`.`menu_ids`)
  AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(@mes_work_order_create_erp_menu_id AS JSON), '$');

INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT `source_role_menu`.`role_id`, @mes_work_order_create_erp_menu_id,
       '', NOW(), '', NOW(), b'0', `source_role_menu`.`tenant_id`
FROM `system_role_menu` AS `source_role_menu`
WHERE `source_role_menu`.`menu_id` = 5530
  AND `source_role_menu`.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `source_role_menu`.`role_id`
        AND `existing`.`menu_id` = @mes_work_order_create_erp_menu_id
        AND `existing`.`tenant_id` = `source_role_menu`.`tenant_id`
        AND `existing`.`deleted` = b'0'
  );

INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT `role`.`id`, @mes_work_order_create_erp_menu_id,
       '', NOW(), '', NOW(), b'0', `tenant`.`id`
FROM `system_tenant` AS `tenant`
INNER JOIN `system_users` AS `user`
        ON `user`.`tenant_id` = `tenant`.`id`
       AND `user`.`username` = 'aoteman'
       AND `user`.`deleted` = b'0'
INNER JOIN `system_user_role` AS `user_role`
        ON `user_role`.`user_id` = `user`.`id`
       AND `user_role`.`tenant_id` = `tenant`.`id`
       AND `user_role`.`deleted` = b'0'
INNER JOIN `system_role` AS `role`
        ON `role`.`id` = `user_role`.`role_id`
       AND `role`.`tenant_id` = `tenant`.`id`
       AND `role`.`code` = 'tenant_admin'
       AND `role`.`deleted` = b'0'
WHERE `tenant`.`name` = '测试租户'
  AND `tenant`.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = @mes_work_order_create_erp_menu_id
        AND `existing`.`tenant_id` = `tenant`.`id`
        AND `existing`.`deleted` = b'0'
  );

SET @mes_kingdee_work_order_unique_exists := (
    SELECT COUNT(*)
    FROM `information_schema`.`STATISTICS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'mes_kingdee_production_order_sync_record'
      AND `INDEX_NAME` = 'uk_mes_kingdee_production_work_order'
);

SET @mes_kingdee_work_order_unique_sql := IF(
    @mes_kingdee_work_order_unique_exists = 0,
    'ALTER TABLE `mes_kingdee_production_order_sync_record` ADD UNIQUE INDEX `uk_mes_kingdee_production_work_order` (`tenant_id`, `work_order_id`, `deleted`)',
    'SELECT ''uk_mes_kingdee_production_work_order already exists'' AS message'
);

PREPARE mes_kingdee_work_order_unique_stmt FROM @mes_kingdee_work_order_unique_sql;
EXECUTE mes_kingdee_work_order_unique_stmt;
DEALLOCATE PREPARE mes_kingdee_work_order_unique_stmt;

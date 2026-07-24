-- release-migration: allowedEnvironments=test,backup; dependsOn=20260619_post_release_role_e2e_gate_smoke_username_password_freshness_fix; type=permission; riskLevel=medium
-- Surface the ERP smoke manual sync button without exposing the Infra job menu tree.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_smoke_erp_job_permission_fix;

DELIMITER $$

CREATE PROCEDURE apply_post_release_role_e2e_gate_smoke_erp_job_permission_fix()
BEGIN
  DECLARE admin_tenant_id BIGINT DEFAULT NULL;
  DECLARE erp_creator_role_id BIGINT DEFAULT NULL;
  DECLARE erp_sync_menu_id BIGINT DEFAULT NULL;
  DECLARE erp_job_query_menu_id BIGINT DEFAULT NULL;
  DECLARE erp_job_trigger_menu_id BIGINT DEFAULT NULL;
  DECLARE erp_job_permission_count INT DEFAULT 0;
  DECLARE erp_job_role_menu_count INT DEFAULT 0;

  SELECT `id` INTO admin_tenant_id
  FROM `system_tenant`
  WHERE `name` = '芋道源码'
    AND `status` = 0
    AND `deleted` = b'0'
  LIMIT 1;

  IF admin_tenant_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release ERP job permission fix requires enabled tenant 芋道源码';
  END IF;

  SELECT `id` INTO erp_creator_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id
    AND `code` = 'post_release_mes_smoke_erp_creator'
    AND `status` = 0
    AND `deleted` = b'0'
  LIMIT 1;

  IF erp_creator_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release ERP job permission fix requires enabled ERP smoke creator role';
  END IF;

  SELECT `id` INTO erp_sync_menu_id
  FROM `system_menu`
  WHERE `id` = 6013
    AND `name` = '金蝶同步运行'
    AND `parent_id` = 2563
    AND `status` = 0
    AND `deleted` = b'0'
  LIMIT 1;

  IF erp_sync_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release ERP job permission fix requires ERP Kingdee sync menu 6013';
  END IF;

  INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT 'ERP同步任务查询', 'infra:job:query', 3, 2, erp_sync_menu_id, '', '', '', '',
         0, b'1', b'1', b'1', 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `parent_id` = erp_sync_menu_id
      AND `permission` = 'infra:job:query'
      AND `deleted` = b'0'
  );

  INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT 'ERP同步任务触发', 'infra:job:trigger', 3, 3, erp_sync_menu_id, '', '', '', '',
         0, b'1', b'1', b'1', 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `parent_id` = erp_sync_menu_id
      AND `permission` = 'infra:job:trigger'
      AND `deleted` = b'0'
  );

  UPDATE `system_menu`
  SET `name` = CASE `permission`
        WHEN 'infra:job:query' THEN 'ERP同步任务查询'
        WHEN 'infra:job:trigger' THEN 'ERP同步任务触发'
        ELSE `name`
      END,
      `type` = 3,
      `status` = 0,
      `visible` = b'1',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `parent_id` = erp_sync_menu_id
    AND `permission` IN ('infra:job:query', 'infra:job:trigger')
    AND `deleted` = b'0';

  SELECT `id` INTO erp_job_query_menu_id
  FROM `system_menu`
  WHERE `parent_id` = erp_sync_menu_id
    AND `permission` = 'infra:job:query'
    AND `type` = 3
    AND `status` = 0
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;

  SELECT `id` INTO erp_job_trigger_menu_id
  FROM `system_menu`
  WHERE `parent_id` = erp_sync_menu_id
    AND `permission` = 'infra:job:trigger'
    AND `type` = 3
    AND `status` = 0
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;

  IF erp_job_query_menu_id IS NULL OR erp_job_trigger_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release ERP job permission fix failed to create ERP-scoped job buttons';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_erp_job_permission_menus`;
  CREATE TEMPORARY TABLE `tmp_post_release_erp_job_permission_menus` (
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`menu_id`)
  );

  INSERT INTO `tmp_post_release_erp_job_permission_menus` (`menu_id`) VALUES
    (erp_job_query_menu_id),
    (erp_job_trigger_menu_id);

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT erp_creator_role_id, `menu_id`, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  FROM `tmp_post_release_erp_job_permission_menus` AS `target_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `tenant_id` = admin_tenant_id
      AND `role_id` = erp_creator_role_id
      AND `menu_id` = `target_menu`.`menu_id`
      AND `deleted` = b'0'
  );

  SELECT COUNT(DISTINCT `permission`) INTO erp_job_permission_count
  FROM `system_menu`
  WHERE `parent_id` = erp_sync_menu_id
    AND `permission` IN ('infra:job:query', 'infra:job:trigger')
    AND `type` = 3
    AND `status` = 0
    AND `deleted` = b'0';

  SELECT COUNT(DISTINCT sm.`permission`) INTO erp_job_role_menu_count
  FROM `system_role_menu` rm
  INNER JOIN `system_menu` sm ON sm.`id` = rm.`menu_id`
  WHERE rm.`tenant_id` = admin_tenant_id
    AND rm.`role_id` = erp_creator_role_id
    AND rm.`deleted` = b'0'
    AND sm.`parent_id` = erp_sync_menu_id
    AND sm.`permission` IN ('infra:job:query', 'infra:job:trigger')
    AND sm.`type` = 3
    AND sm.`status` = 0
    AND sm.`deleted` = b'0';

  IF erp_job_permission_count <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release ERP job permission fix did not prepare ERP-scoped job permissions';
  END IF;

  IF erp_job_role_menu_count <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release ERP job permission fix did not bind ERP smoke role to job permissions';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_erp_job_permission_menus`;
END$$

DELIMITER ;

CALL apply_post_release_role_e2e_gate_smoke_erp_job_permission_fix();

DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_smoke_erp_job_permission_fix;

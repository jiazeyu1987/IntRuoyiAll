-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260725_system_backup_plan_menu; type=menu; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_backup_evidence_permission;

DELIMITER //
CREATE PROCEDURE ensure_backup_evidence_permission()
BEGIN
  DECLARE v_menu_id BIGINT DEFAULT NULL;
  DECLARE v_export_id BIGINT DEFAULT NULL;

  SELECT `id` INTO v_menu_id
  FROM `system_menu`
  WHERE `permission` = 'system:backup-plan:query'
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;

  IF v_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing system backup plan menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 901103
      AND (`deleted` <> b'0' OR `permission` <> 'system:backup-plan:evidence-export')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting system_menu id 901103';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    901103, '导出备份审查证据', 'system:backup-plan:evidence-export', 3, 3, v_menu_id,
    '', '', '', '', 0, b'1', b'1', b'1', 'codex', NOW(), 'codex', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 901103 OR `permission` = 'system:backup-plan:evidence-export')
  );

  SELECT `id` INTO v_export_id
  FROM `system_menu`
  WHERE `permission` = 'system:backup-plan:evidence-export'
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;

  IF v_export_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing backup evidence export permission';
  END IF;

  UPDATE `system_menu`
  SET `parent_id` = v_menu_id,
      `type` = 3,
      `sort` = 3,
      `status` = 0,
      `visible` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = v_export_id;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  UPDATE `system_tenant_package`
  SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', v_export_id),
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND JSON_VALID(`menu_ids`)
    AND JSON_CONTAINS(`menu_ids`, CAST(v_menu_id AS JSON), '$')
    AND NOT JSON_CONTAINS(`menu_ids`, CAST(v_export_id AS JSON), '$');

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`, v_export_id, 'codex', NOW(), 'codex', NOW(), b'0', `role`.`tenant_id`
  FROM `system_role` AS `role`
  LEFT JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  LEFT JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND (
      `role`.`code` = 'super_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(v_export_id AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = v_export_id
        AND `existing`.`deleted` = b'0'
    );
END//
DELIMITER ;

CALL ensure_backup_evidence_permission();

DROP PROCEDURE IF EXISTS ensure_backup_evidence_permission;

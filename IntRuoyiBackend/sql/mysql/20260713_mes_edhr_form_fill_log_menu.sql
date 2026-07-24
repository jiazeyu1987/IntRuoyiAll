-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260702_mes_edhr_seven_visible_tabs; type=menu; riskLevel=low
-- eDHR form log read-only menu and permission baseline.
-- Fail-fast migration: no fallback, no write permission, no mock role binding.
SET NAMES utf8mb4;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900432, '表单日志', 'mes:pro-edhr-form-fill-log:query', 2, 6, 900220, '/mes/pro/feedback/edhr-form-fill-log', 'ep:document-copy', 'mes/pro/edhr/FormFillLogPage', 'MesProEdhrFormFillLogPage', 0, b'1', b'1', b'1', 'edhr-form-fill-log-menu', NOW(), 'edhr-form-fill-log-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900432 OR `path` = '/mes/pro/feedback/edhr-form-fill-log');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900433, '表单日志查询', 'mes:pro-edhr-form-fill-log:query', 3, 1, 900432, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-fill-log-menu', NOW(), 'edhr-form-fill-log-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-fill-log:query' AND `type` = 3);

UPDATE `system_menu`
SET `name` = '表单日志',
    `sort` = 6,
    `parent_id` = 900220,
    `path` = '/mes/pro/feedback/edhr-form-fill-log',
    `icon` = 'ep:document-copy',
    `component` = 'mes/pro/edhr/FormFillLogPage',
    `component_name` = 'MesProEdhrFormFillLogPage',
    `permission` = 'mes:pro-edhr-form-fill-log:query',
    `updater` = 'edhr-form-fill-log-menu',
    `update_time` = NOW()
WHERE `id` = 900432
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 7,
    `updater` = 'edhr-form-fill-log-menu',
    `update_time` = NOW()
WHERE `id` = 900303
  AND `parent_id` = 900220
  AND `sort` <= 6
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '表单日志查询',
    `permission` = 'mes:pro-edhr-form-fill-log:query',
    `type` = 3,
    `sort` = 1,
    `parent_id` = 900432,
    `updater` = 'edhr-form-fill-log-menu',
    `update_time` = NOW()
WHERE `id` = 900433
  AND `deleted` = b'0';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_fill_log_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_form_fill_log_menus()
BEGIN
  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR form fill log menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900432, 900433)) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR form fill log system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_fill_log_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_fill_log_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_fill_log_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_fill_log_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900432, 900433)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_fill_log_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_fill_log_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_form_fill_log_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_form_fill_log_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_edhr_form_fill_log_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_form_fill_log_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_form_fill_log_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_edhr_form_fill_log_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-form-fill-log-menu',
      `package`.`update_time` = NOW();

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role`
    ON `role`.`id` = `role_menu`.`role_id`
   AND `role`.`tenant_id` = `role_menu`.`tenant_id`
   AND `role`.`deleted` = b'0'
   AND `role`.`code` = 'tenant_admin'
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_form_fill_log_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `tmp_mes_edhr_form_fill_log_menu_ids` AS `menu`
    ON `menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-form-fill-log-menu',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-form-fill-log-menu',
    NOW(),
    'edhr-form-fill-log-menu',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_form_fill_log_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `tmp_mes_edhr_form_fill_log_menu_ids` AS `menu`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'tenant_admin'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_fill_log_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_fill_log_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_fill_log_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_form_fill_log_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_fill_log_menus;

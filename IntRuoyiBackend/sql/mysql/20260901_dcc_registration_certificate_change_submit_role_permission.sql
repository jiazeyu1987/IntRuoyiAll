-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260816_dcc_registration_certificate_menu; type=data; riskLevel=low
-- Purpose: Allow existing registration-certificate upload or renewal maintainers to submit change approval applications.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TEMPORARY TABLE IF NOT EXISTS `tmp_dcc_reg_cert_change_submit_roles` (
  `role_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `tenant_id`)
) ENGINE=MEMORY;

TRUNCATE TABLE `tmp_dcc_reg_cert_change_submit_roles`;

INSERT INTO `tmp_dcc_reg_cert_change_submit_roles` (`role_id`, `tenant_id`)
SELECT DISTINCT `role_menu`.`role_id`, `role_menu`.`tenant_id`
FROM `system_role_menu` AS `role_menu`
INNER JOIN `system_role` AS `role`
        ON `role`.`id` = `role_menu`.`role_id`
       AND `role`.`tenant_id` = `role_menu`.`tenant_id`
       AND `role`.`deleted` = b'0'
       AND `role`.`status` = 0
INNER JOIN `system_menu` AS `source_menu`
        ON `source_menu`.`id` = `role_menu`.`menu_id`
       AND `source_menu`.`deleted` = b'0'
       AND `source_menu`.`status` = 0
WHERE `role_menu`.`deleted` = b'0'
  AND `source_menu`.`permission` IN (
    'dcc:registration-certificate:upload:create',
    'dcc:registration-certificate:renewal:upload'
  );

UPDATE `system_role_menu` AS `role_menu`
INNER JOIN `tmp_dcc_reg_cert_change_submit_roles` AS `source_role`
        ON `source_role`.`role_id` = `role_menu`.`role_id`
       AND `source_role`.`tenant_id` = `role_menu`.`tenant_id`
INNER JOIN `system_menu` AS `target_menu`
        ON `target_menu`.`id` = `role_menu`.`menu_id`
       AND `target_menu`.`permission` = 'dcc:registration-certificate:change:submit'
       AND `target_menu`.`deleted` = b'0'
       AND `target_menu`.`status` = 0
SET `role_menu`.`deleted` = b'0',
    `role_menu`.`updater` = 'registration-change-permission',
    `role_menu`.`update_time` = NOW()
WHERE `role_menu`.`deleted` = b'1';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `source_role`.`role_id`,
       `target_menu`.`id`,
       'registration-change-permission',
       NOW(),
       'registration-change-permission',
       NOW(),
       b'0',
       `source_role`.`tenant_id`
FROM `tmp_dcc_reg_cert_change_submit_roles` AS `source_role`
INNER JOIN `system_menu` AS `target_menu`
        ON `target_menu`.`permission` = 'dcc:registration-certificate:change:submit'
       AND `target_menu`.`deleted` = b'0'
       AND `target_menu`.`status` = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `source_role`.`role_id`
      AND `existing`.`menu_id` = `target_menu`.`id`
      AND `existing`.`tenant_id` = `source_role`.`tenant_id`
      AND `existing`.`deleted` = b'0'
);

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_change_submit_roles`;

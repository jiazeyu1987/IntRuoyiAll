-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260708_mes_batch_record_version_phase_one; type=menu; riskLevel=low
-- Remove the frontend eDHR version governance page menu while preserving backend governance permissions and APIs.
-- Fail-fast migration: no hard delete, no backend capability removal, no fallback route.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_version_governance_menu_removed;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_version_governance_menu_removed()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 900220
        AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch record parent menu 900220; cannot remove version governance page safely';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot remove eDHR version governance page menu';
  END IF;

  UPDATE `system_menu`
  SET `parent_id` = 900220,
      `path` = '',
      `component` = '',
      `component_name` = '',
      `visible` = b'0',
      `keep_alive` = b'0',
      `always_show` = b'0',
      `updater` = 'edhr-version-governance-menu-removal',
      `update_time` = NOW()
  WHERE `id` IN (900304, 900305, 900306)
    AND `type` = 3
    AND `permission` IN (
      'mes:pro-batch-record-version:confirm',
      'mes:pro-batch-record-version:import',
      'mes:pro-batch-record-version:rollback-request'
    )
    AND `deleted` = b'0';

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `role_menu`.`menu_id`
   AND `menu`.`id` = 900303
   AND `menu`.`permission` = 'mes:pro-batch-record-version:governance-query'
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'edhr-version-governance-menu-removal',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'0';

  UPDATE `system_menu`
  SET `status` = 1,
      `visible` = b'0',
      `keep_alive` = b'0',
      `always_show` = b'0',
      `deleted` = b'1',
      `updater` = 'edhr-version-governance-menu-removal',
      `update_time` = NOW()
  WHERE `id` = 900303
    AND `name` = 'eDHR版本治理'
    AND `permission` = 'mes:pro-batch-record-version:governance-query'
    AND `type` = 2
    AND `path` = '/mes/pro/feedback/edhr-version-governance'
    AND `component` = 'mes/pro/edhr-version-governance/VersionGovernancePage'
    AND `component_name` = 'MesProEdhrVersionGovernancePage'
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_version_governance_affected_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_version_governance_affected_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900303' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_version_governance_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_version_governance_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_version_governance_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `affected`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_version_governance_affected_packages` AS `affected`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `affected`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `existing_menu`.`menu_id` <> 900303;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_version_governance_affected_packages` AS `affected`
    ON `affected`.`package_id` = `package`.`id`
  LEFT JOIN (
    SELECT
      `ordered`.`package_id`,
      JSON_ARRAYAGG(`ordered`.`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_version_governance_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `ordered`
    GROUP BY `ordered`.`package_id`
  ) AS `filtered`
    ON `filtered`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = COALESCE(`filtered`.`menu_ids`, JSON_ARRAY()),
      `package`.`updater` = 'edhr-version-governance-menu-removal',
      `package`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_version_governance_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_version_governance_affected_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_version_governance_menu_removed();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_version_governance_menu_removed;

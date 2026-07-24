-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_workstation_default_single_capacity_30`;
CREATE TEMPORARY TABLE `tmp_mes_workstation_default_single_capacity_30` AS
SELECT
    `id`,
    `code`,
    `name`,
    `process_id`,
    `tenant_id`,
    `single_standard_hourly_capacity`
FROM `mes_md_workstation`
WHERE `tenant_id` = 1
  AND `deleted` = b'0'
  AND `single_standard_hourly_capacity` IS NULL;

SELECT COUNT(*) AS pending_update_count
FROM `tmp_mes_workstation_default_single_capacity_30`;

UPDATE `mes_md_workstation`
SET `single_standard_hourly_capacity` = 30.00
WHERE `tenant_id` = 1
  AND `deleted` = b'0'
  AND `single_standard_hourly_capacity` IS NULL
  AND `id` IN (
      SELECT `id`
      FROM `tmp_mes_workstation_default_single_capacity_30`
  );

SELECT COUNT(*) AS updated_to_default_count
FROM `mes_md_workstation` `w`
INNER JOIN `tmp_mes_workstation_default_single_capacity_30` `t`
        ON `t`.`id` = `w`.`id`
WHERE `w`.`tenant_id` = 1
  AND `w`.`deleted` = b'0'
  AND `w`.`single_standard_hourly_capacity` = 30.00;

SELECT COUNT(*) AS remaining_missing_count
FROM `mes_md_workstation`
WHERE `tenant_id` = 1
  AND `deleted` = b'0'
  AND `single_standard_hourly_capacity` IS NULL;

COMMIT;

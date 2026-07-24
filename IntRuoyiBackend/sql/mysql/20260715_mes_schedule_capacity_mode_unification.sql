-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=data; riskLevel=high
-- 统一智能排产产能口径：FINITE_HOURLY 迁移为 MANUAL_OVERRIDE，并将可证明的设备主数据产能回填到设备工序产能。

DROP PROCEDURE IF EXISTS `assert_mes_schedule_capacity_mode_unification`;
DELIMITER $$
CREATE PROCEDURE `assert_mes_schedule_capacity_mode_unification`()
BEGIN
    DECLARE invalid_capacity_mode_count INT DEFAULT 0;
    DECLARE invalid_manual_override_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO invalid_capacity_mode_count
    FROM `mes_pro_route_schedule_config`
    WHERE `deleted` = b'0'
      AND `capacity_mode` NOT IN ('RESOURCE_CALCULATED', 'MANUAL_OVERRIDE', 'INFINITE_FORMULA');

    SELECT COUNT(*)
      INTO invalid_manual_override_count
    FROM `mes_pro_route_schedule_config`
    WHERE `deleted` = b'0'
      AND `capacity_mode` = 'MANUAL_OVERRIDE'
      AND (`hourly_capacity` IS NULL OR `hourly_capacity` <= 0);

    IF invalid_capacity_mode_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'capacity mode unification found invalid route schedule capacity mode';
    END IF;

    IF invalid_manual_override_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'capacity mode unification found manual override without positive hourly capacity';
    END IF;
END$$
DELIMITER ;

START TRANSACTION;

SET @mes_capacity_unification_legacy_before_count := (
    SELECT COUNT(*)
    FROM `mes_pro_route_schedule_config`
    WHERE `capacity_mode` = 'FINITE_HOURLY'
      AND `deleted` = b'0'
);

SET @mes_capacity_unification_manual_review_rows := (
    SELECT COUNT(*)
    FROM `mes_pro_route_schedule_config`
    WHERE `deleted` = b'0'
      AND `capacity_mode` IN ('FINITE_HOURLY', 'MANUAL_OVERRIDE')
      AND (`hourly_capacity` IS NULL OR `hourly_capacity` <= 0)
);

SET @mes_capacity_unification_backfill_candidates := (
    SELECT COUNT(DISTINCT workstation_machine.`machinery_id`, workstation.`process_id`, workstation_machine.`tenant_id`)
    FROM `mes_md_workstation_machine` workstation_machine
    JOIN `mes_md_workstation` workstation
      ON workstation.`id` = workstation_machine.`workstation_id`
     AND workstation.`tenant_id` = workstation_machine.`tenant_id`
     AND workstation.`deleted` = b'0'
    JOIN `mes_dv_machinery` master_machinery
      ON master_machinery.`id` = workstation_machine.`machinery_id`
     AND master_machinery.`tenant_id` = workstation_machine.`tenant_id`
     AND master_machinery.`deleted` = b'0'
    LEFT JOIN `mes_dv_machinery_process` process_capacity
      ON process_capacity.`machinery_id` = workstation_machine.`machinery_id`
     AND process_capacity.`process_id` = workstation.`process_id`
     AND process_capacity.`tenant_id` = workstation_machine.`tenant_id`
     AND process_capacity.`deleted` = b'0'
    WHERE workstation_machine.`deleted` = b'0'
      AND workstation.`process_id` IS NOT NULL
      AND process_capacity.`id` IS NULL
      AND master_machinery.`standard_hourly_capacity` IS NOT NULL
      AND master_machinery.`standard_hourly_capacity` > 0
);

UPDATE `mes_pro_route_schedule_config`
SET `capacity_mode` = 'MANUAL_OVERRIDE',
    `update_time` = NOW()
WHERE `capacity_mode` = 'FINITE_HOURLY'
  AND `deleted` = b'0';

SET @mes_capacity_unification_migrated_rows := ROW_COUNT();

INSERT INTO `mes_dv_machinery_process` (
    `machinery_id`,
    `process_id`,
    `machinery_code`,
    `device_name`,
    `standard_hourly_capacity`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`,
    `tenant_id`
)
SELECT DISTINCT
    workstation_machine.`machinery_id`,
    workstation.`process_id`,
    master_machinery.`code`,
    master_machinery.`name`,
    master_machinery.`standard_hourly_capacity`,
    'capacity mode unification backfill from machinery master',
    'migration',
    NOW(),
    'migration',
    NOW(),
    b'0',
    workstation_machine.`tenant_id`
FROM `mes_md_workstation_machine` workstation_machine
JOIN `mes_md_workstation` workstation
  ON workstation.`id` = workstation_machine.`workstation_id`
 AND workstation.`tenant_id` = workstation_machine.`tenant_id`
 AND workstation.`deleted` = b'0'
JOIN `mes_dv_machinery` master_machinery
  ON master_machinery.`id` = workstation_machine.`machinery_id`
 AND master_machinery.`tenant_id` = workstation_machine.`tenant_id`
 AND master_machinery.`deleted` = b'0'
LEFT JOIN `mes_dv_machinery_process` process_capacity
  ON process_capacity.`machinery_id` = workstation_machine.`machinery_id`
 AND process_capacity.`process_id` = workstation.`process_id`
 AND process_capacity.`tenant_id` = workstation_machine.`tenant_id`
 AND process_capacity.`deleted` = b'0'
WHERE workstation_machine.`deleted` = b'0'
  AND workstation.`process_id` IS NOT NULL
  AND process_capacity.`id` IS NULL
  AND master_machinery.`standard_hourly_capacity` IS NOT NULL
  AND master_machinery.`standard_hourly_capacity` > 0
  AND NOT EXISTS (
      SELECT 1
      FROM `mes_dv_machinery_process` duplicate_process
      WHERE duplicate_process.`machinery_id` = workstation_machine.`machinery_id`
        AND duplicate_process.`process_id` = workstation.`process_id`
        AND duplicate_process.`tenant_id` = workstation_machine.`tenant_id`
        AND duplicate_process.`deleted` = b'0'
  );

SET @mes_capacity_unification_machinery_process_backfill_rows := ROW_COUNT();
SET @mes_capacity_unification_machinery_process_backfill_skipped_rows :=
    GREATEST(@mes_capacity_unification_backfill_candidates
        - @mes_capacity_unification_machinery_process_backfill_rows, 0);

SELECT
    @mes_capacity_unification_legacy_before_count AS legacy_finite_hourly_before_count,
    @mes_capacity_unification_migrated_rows AS migrated_finite_hourly_rows,
    @mes_capacity_unification_machinery_process_backfill_rows AS machinery_process_backfill_rows,
    @mes_capacity_unification_machinery_process_backfill_skipped_rows AS machinery_process_backfill_skipped_rows,
    @mes_capacity_unification_manual_review_rows AS manual_review_rows;

CALL `assert_mes_schedule_capacity_mode_unification`();

COMMIT;

DROP PROCEDURE IF EXISTS `assert_mes_schedule_capacity_mode_unification`;

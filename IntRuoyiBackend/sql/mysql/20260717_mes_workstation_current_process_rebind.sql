-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_mes_base_schema; type=data; riskLevel=medium
-- Goal:
--   Rebind workstation master data from deleted historical process rows to the current active process row
--   with the same tenant_id + process code, then backfill route-process workstation_id when the current
--   process has exactly one active workstation.
-- Safety:
--   This migration fails fast when one historical process code maps to multiple active current processes.
--   It does not clone workstation master data and does not infer bindings from names.
-- Rollback:
--   Restore mes_md_workstation.process_id from mes_md_workstation_process_rebind_20260717.
--   Clear route_process.workstation_id values listed in mes_route_process_workstation_rebind_20260717 if needed.

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_mes_workstation_current_process_rebind`$$
CREATE PROCEDURE `migrate_mes_workstation_current_process_rebind`()
BEGIN
    DECLARE ambiguous_active_process_code_count bigint DEFAULT 0;
    DECLARE rebind_candidate_workstation_count bigint DEFAULT 0;
    DECLARE rebound_workstation_count bigint DEFAULT 0;
    DECLARE route_process_backfill_candidate_count bigint DEFAULT 0;
    DECLARE route_process_backfilled_count bigint DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_workstation_ambiguous_active_process_code`;
    CREATE TEMPORARY TABLE `tmp_mes_workstation_ambiguous_active_process_code` AS
    SELECT
        old_process.`tenant_id`,
        old_process.`code`,
        COUNT(DISTINCT current_process.`id`) AS `active_process_count`,
        GROUP_CONCAT(DISTINCT current_process.`id` ORDER BY current_process.`id`) AS `active_process_ids`
      FROM `mes_md_workstation` workstation
      JOIN `mes_pro_process` old_process
        ON old_process.`id` = workstation.`process_id`
       AND old_process.`tenant_id` = workstation.`tenant_id`
       AND old_process.`deleted` = b'1'
      JOIN `mes_pro_process` current_process
        ON current_process.`tenant_id` = old_process.`tenant_id`
       AND current_process.`code` = old_process.`code`
       AND current_process.`deleted` = b'0'
     WHERE workstation.`deleted` = b'0'
       AND old_process.`code` IS NOT NULL
       AND old_process.`code` <> ''
     GROUP BY old_process.`tenant_id`, old_process.`code`
    HAVING COUNT(DISTINCT current_process.`id`) > 1;

    SELECT COUNT(*)
      INTO ambiguous_active_process_code_count
      FROM `tmp_mes_workstation_ambiguous_active_process_code`;

    IF ambiguous_active_process_code_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'workstation historical process code maps to multiple active processes';
    END IF;

    CREATE TABLE IF NOT EXISTS `mes_md_workstation_process_rebind_20260717` (
        `workstation_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `old_process_id` bigint NOT NULL,
        `new_process_id` bigint NOT NULL,
        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`workstation_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='20260717 工作站历史工序迁移到当前工序备份';

    CREATE TABLE IF NOT EXISTS `mes_route_process_workstation_rebind_20260717` (
        `route_process_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `process_id` bigint NOT NULL,
        `workstation_id` bigint NOT NULL,
        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`route_process_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='20260717 路线工序工作站回填备份';

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_workstation_current_process_rebind`;
    CREATE TEMPORARY TABLE `tmp_mes_workstation_current_process_rebind` AS
    SELECT
        workstation.`id` AS `workstation_id`,
        workstation.`tenant_id`,
        workstation.`process_id` AS `old_process_id`,
        MIN(current_process.`id`) AS `new_process_id`
      FROM `mes_md_workstation` workstation
      JOIN `mes_pro_process` old_process
        ON old_process.`id` = workstation.`process_id`
       AND old_process.`tenant_id` = workstation.`tenant_id`
       AND old_process.`deleted` = b'1'
      JOIN `mes_pro_process` current_process
        ON current_process.`tenant_id` = old_process.`tenant_id`
       AND current_process.`code` = old_process.`code`
       AND current_process.`deleted` = b'0'
     WHERE workstation.`deleted` = b'0'
       AND old_process.`code` IS NOT NULL
       AND old_process.`code` <> ''
       AND workstation.`process_id` <> current_process.`id`
     GROUP BY workstation.`id`, workstation.`tenant_id`, workstation.`process_id`
    HAVING COUNT(DISTINCT current_process.`id`) = 1;

    SELECT COUNT(*)
      INTO rebind_candidate_workstation_count
      FROM `tmp_mes_workstation_current_process_rebind`;

    START TRANSACTION;

    INSERT IGNORE INTO `mes_md_workstation_process_rebind_20260717` (
        `workstation_id`,
        `tenant_id`,
        `old_process_id`,
        `new_process_id`
    )
    SELECT
        rebind.`workstation_id`,
        rebind.`tenant_id`,
        rebind.`old_process_id`,
        rebind.`new_process_id`
      FROM `tmp_mes_workstation_current_process_rebind` rebind;

    UPDATE `mes_md_workstation` workstation
      JOIN `tmp_mes_workstation_current_process_rebind` rebind
        ON rebind.`workstation_id` = workstation.`id`
       AND rebind.`tenant_id` = workstation.`tenant_id`
       SET workstation.`process_id` = rebind.`new_process_id`,
           workstation.`update_time` = CURRENT_TIMESTAMP
     WHERE workstation.`deleted` = b'0'
       AND workstation.`process_id` = rebind.`old_process_id`;

    SET rebound_workstation_count = ROW_COUNT();

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_current_process_unique_workstation`;
    CREATE TEMPORARY TABLE `tmp_mes_current_process_unique_workstation` AS
    SELECT
        workstation.`tenant_id`,
        workstation.`process_id`,
        MIN(workstation.`id`) AS `workstation_id`
      FROM `mes_md_workstation` workstation
     WHERE workstation.`deleted` = b'0'
       AND workstation.`process_id` IS NOT NULL
     GROUP BY workstation.`tenant_id`, workstation.`process_id`
    HAVING COUNT(DISTINCT workstation.`id`) = 1;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_process_workstation_rebind`;
    CREATE TEMPORARY TABLE `tmp_mes_route_process_workstation_rebind` AS
    SELECT
        route_process.`id` AS `route_process_id`,
        route_process.`tenant_id`,
        route_process.`process_id`,
        unique_workstation.`workstation_id`
      FROM `mes_pro_route_process` route_process
      JOIN `tmp_mes_current_process_unique_workstation` unique_workstation
        ON unique_workstation.`tenant_id` = route_process.`tenant_id`
       AND unique_workstation.`process_id` = route_process.`process_id`
     WHERE route_process.`deleted` = b'0'
       AND route_process.`workstation_id` IS NULL;

    SELECT COUNT(*)
      INTO route_process_backfill_candidate_count
      FROM `tmp_mes_route_process_workstation_rebind`;

    INSERT IGNORE INTO `mes_route_process_workstation_rebind_20260717` (
        `route_process_id`,
        `tenant_id`,
        `process_id`,
        `workstation_id`
    )
    SELECT
        route_rebind.`route_process_id`,
        route_rebind.`tenant_id`,
        route_rebind.`process_id`,
        route_rebind.`workstation_id`
      FROM `tmp_mes_route_process_workstation_rebind` route_rebind;

    UPDATE `mes_pro_route_process` route_process
      JOIN `tmp_mes_route_process_workstation_rebind` unique_workstation
        ON unique_workstation.`route_process_id` = route_process.`id`
       AND unique_workstation.`tenant_id` = route_process.`tenant_id`
       SET route_process.`workstation_id` = unique_workstation.`workstation_id`,
           route_process.`update_time` = CURRENT_TIMESTAMP
     WHERE route_process.`deleted` = b'0'
       AND route_process.`workstation_id` IS NULL;

    SET route_process_backfilled_count = ROW_COUNT();

    COMMIT;

    SELECT
        rebind_candidate_workstation_count AS `rebind_candidate_workstation_count`,
        rebound_workstation_count AS `rebound_workstation_count`,
        route_process_backfill_candidate_count AS `route_process_backfill_candidate_count`,
        route_process_backfilled_count AS `route_process_backfilled_count`,
        ambiguous_active_process_code_count AS `ambiguous_active_process_code_count`;
END$$

DELIMITER ;

CALL `migrate_mes_workstation_current_process_rebind`();

DROP PROCEDURE IF EXISTS `migrate_mes_workstation_current_process_rebind`;

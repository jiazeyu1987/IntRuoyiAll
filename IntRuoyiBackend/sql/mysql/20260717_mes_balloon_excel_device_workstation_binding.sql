-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260708_mes_balloon_process_device_capacity; type=data; riskLevel=medium
-- Goal:
--   Create or reuse one current-process workstation for each Excel route process in ROUTE-XLSX-00001/00002,
--   attach the Excel device rows as workstation machine resources, and bind mes_pro_route_process.workstation_id.
-- Safety:
--   The script is scoped to local tenant_id=1 target route codes. Existing workstation or route-process conflicts
--   fail fast instead of silently overwriting business data.
-- Rollback:
--   Use mes_balloon_excel_workstation_binding_20260717 to restore route_process.workstation_id.
--   Use mes_balloon_excel_workstation_created_20260717 and
--   mes_balloon_excel_workstation_machine_created_20260717 to identify rows inserted by this migration.

SET @target_tenant_id = 1;
SET @target_route_process_count = 49;
SET @target_workstation_count = 49;
SET @target_workstation_machine_count = 83;
SET @target_shift_hours = 10.50;

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_mes_balloon_excel_device_workstation_binding`$$
CREATE PROCEDURE `migrate_mes_balloon_excel_device_workstation_binding`()
BEGIN
    DECLARE v_default_workshop_id bigint DEFAULT NULL;
    DECLARE v_default_production_line_id bigint DEFAULT NULL;
    DECLARE v_target_route_process_count int DEFAULT 0;
    DECLARE v_target_workstation_count int DEFAULT 0;
    DECLARE v_target_workstation_machine_count int DEFAULT 0;
    DECLARE v_inserted_workstation_count int DEFAULT 0;
    DECLARE v_inserted_workstation_machine_count int DEFAULT 0;
    DECLARE v_bound_route_process_count int DEFAULT 0;
    DECLARE final_bound_route_process_count int DEFAULT 0;
    DECLARE final_missing_workstation_count int DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT `id`
      INTO v_default_workshop_id
      FROM `mes_md_workshop`
     WHERE `tenant_id` = @target_tenant_id
       AND `deleted` = b'0'
     ORDER BY `id`
     LIMIT 1;

    IF v_default_workshop_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing default workshop for balloon Excel workstation binding';
    END IF;

    SELECT `id`
      INTO v_default_production_line_id
      FROM `mes_md_production_line`
     WHERE `tenant_id` = @target_tenant_id
       AND `deleted` = b'0'
     ORDER BY `id`
     LIMIT 1;

    DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_excel_workstation_seed`;
    CREATE TEMPORARY TABLE `tmp_balloon_excel_workstation_seed` AS
    SELECT
        route_process.`tenant_id`,
        route.`code` AS `route_code`,
        route.`name` AS `product_name`,
        route_process.`id` AS `route_process_id`,
        route_process.`process_id`,
        route_process.`sort`,
        process.`code` AS `process_code`,
        process.`name` AS `process_name`,
        CONCAT(REPLACE(route.`code`, 'ROUTE-', 'WS-'), '-', LPAD(route_process.`sort`, 2, '0')) AS `workstation_code`,
        LEFT(CONCAT(route.`name`, '-', process.`name`, '-工作站'), 128) AS `workstation_name`,
        v_default_workshop_id AS `workshop_id`,
        v_default_production_line_id AS `production_line_id`,
        CASE
            WHEN process.`manual_shift_capacity` IS NOT NULL
                THEN process.`manual_shift_capacity` / @target_shift_hours
            ELSE NULL
        END AS `single_standard_hourly_capacity`,
        @target_shift_hours AS `shift_hours`,
        workstation.`id` AS `pre_existing_workstation_id`,
        CAST(NULL AS SIGNED) AS `workstation_id`
      FROM `mes_pro_route` route
      JOIN `mes_pro_route_process` route_process
        ON route_process.`route_id` = route.`id`
       AND route_process.`tenant_id` = route.`tenant_id`
       AND route_process.`deleted` = b'0'
      JOIN `mes_pro_process` process
        ON process.`id` = route_process.`process_id`
       AND process.`tenant_id` = route_process.`tenant_id`
       AND process.`deleted` = b'0'
 LEFT JOIN `mes_md_workstation` workstation
        ON workstation.`tenant_id` = route_process.`tenant_id`
       AND workstation.`deleted` = b'0'
       AND workstation.`code` = CONCAT(REPLACE(route.`code`, 'ROUTE-', 'WS-'), '-', LPAD(route_process.`sort`, 2, '0'))
     WHERE route.`tenant_id` = @target_tenant_id
       AND route.`deleted` = b'0'
       AND route.`code` IN ('ROUTE-XLSX-00001', 'ROUTE-XLSX-00002');

    SELECT COUNT(*)
      INTO v_target_route_process_count
      FROM `tmp_balloon_excel_workstation_seed`;

    IF v_target_route_process_count <> @target_route_process_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'balloon Excel target route process count mismatch';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_excel_workstation_seed` seed
          JOIN `mes_md_workstation` workstation
            ON workstation.`tenant_id` = seed.`tenant_id`
           AND workstation.`deleted` = b'0'
           AND workstation.`code` = seed.`workstation_code`
         WHERE workstation.`process_id` IS NULL
            OR NOT (workstation.`process_id` = seed.`process_id`)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'workstation process conflict';
    END IF;

    CREATE TABLE IF NOT EXISTS `mes_balloon_excel_workstation_binding_20260717` (
        `route_process_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `route_code` varchar(64) NOT NULL,
        `process_id` bigint NOT NULL,
        `old_workstation_id` bigint DEFAULT NULL,
        `new_workstation_id` bigint NOT NULL,
        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`route_process_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='20260717 球囊 Excel 设备转工作站路线绑定备份';

    CREATE TABLE IF NOT EXISTS `mes_balloon_excel_workstation_created_20260717` (
        `workstation_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `workstation_code` varchar(64) NOT NULL,
        `route_process_id` bigint NOT NULL,
        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`workstation_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='20260717 球囊 Excel 设备转工作站新增工作站';

    CREATE TABLE IF NOT EXISTS `mes_balloon_excel_workstation_machine_created_20260717` (
        `workstation_machine_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `workstation_id` bigint NOT NULL,
        `machinery_id` bigint NOT NULL,
        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`workstation_machine_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='20260717 球囊 Excel 设备转工作站新增设备资源';

    START TRANSACTION;

    INSERT INTO `mes_md_workstation` (
        `code`,
        `name`,
        `address`,
        `workshop_id`,
        `process_id`,
        `production_line_id`,
        `single_standard_hourly_capacity`,
        `shift_hours`,
        `status`,
        `remark`,
        `creator`,
        `create_time`,
        `updater`,
        `update_time`,
        `deleted`,
        `tenant_id`
    )
    SELECT
        seed.`workstation_code`,
        seed.`workstation_name`,
        NULL,
        seed.`workshop_id`,
        seed.`process_id`,
        seed.`production_line_id`,
        seed.`single_standard_hourly_capacity`,
        seed.`shift_hours`,
        0,
        CONCAT(seed.`route_code`, ' Excel设备按工作站绑定'),
        'balloon-excel-workstation-binding',
        NOW(),
        'balloon-excel-workstation-binding',
        NOW(),
        b'0',
        seed.`tenant_id`
      FROM `tmp_balloon_excel_workstation_seed` seed
     WHERE seed.`pre_existing_workstation_id` IS NULL;

    SET v_inserted_workstation_count = ROW_COUNT();

    UPDATE `tmp_balloon_excel_workstation_seed` seed
      JOIN `mes_md_workstation` workstation
        ON workstation.`tenant_id` = seed.`tenant_id`
       AND workstation.`deleted` = b'0'
       AND workstation.`code` = seed.`workstation_code`
       SET seed.`workstation_id` = workstation.`id`;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_excel_workstation_seed` seed
         WHERE seed.`workstation_id` IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'balloon Excel workstation creation incomplete';
    END IF;

    SELECT COUNT(DISTINCT `workstation_id`)
      INTO v_target_workstation_count
      FROM `tmp_balloon_excel_workstation_seed`;

    IF v_target_workstation_count <> @target_workstation_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'balloon Excel workstation count mismatch';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_excel_workstation_seed` seed
          JOIN `mes_pro_route_process` route_process
            ON route_process.`id` = seed.`route_process_id`
           AND route_process.`tenant_id` = seed.`tenant_id`
           AND route_process.`deleted` = b'0'
         WHERE route_process.`workstation_id` IS NOT NULL
           AND route_process.`workstation_id` <> seed.`workstation_id`
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target route process workstation conflict';
    END IF;

    INSERT IGNORE INTO `mes_balloon_excel_workstation_created_20260717` (
        `workstation_id`,
        `tenant_id`,
        `workstation_code`,
        `route_process_id`
    )
    SELECT
        seed.`workstation_id`,
        seed.`tenant_id`,
        seed.`workstation_code`,
        seed.`route_process_id`
      FROM `tmp_balloon_excel_workstation_seed` seed
     WHERE seed.`pre_existing_workstation_id` IS NULL;

    DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_excel_workstation_machine_seed`;
    CREATE TEMPORARY TABLE `tmp_balloon_excel_workstation_machine_seed` AS
    SELECT
        seed.`tenant_id`,
        seed.`route_process_id`,
        seed.`workstation_id`,
        mp.`machinery_id`,
        CAST(SUM(mp.`device_quantity`) AS SIGNED) AS `quantity`,
        MIN(existing_binding.`id`) AS `pre_existing_binding_id`
      FROM `tmp_balloon_excel_workstation_seed` seed
      JOIN `mes_dv_machinery_process` mp
        ON mp.`tenant_id` = seed.`tenant_id`
       AND mp.`deleted` = b'0'
       AND mp.`line_name` = seed.`product_name`
       AND mp.`process_id` = seed.`process_id`
       AND mp.`process_code` = seed.`process_code`
      JOIN `mes_dv_machinery` machinery
        ON machinery.`id` = mp.`machinery_id`
       AND machinery.`tenant_id` = mp.`tenant_id`
       AND machinery.`deleted` = b'0'
 LEFT JOIN `mes_md_workstation_machine` existing_binding
        ON existing_binding.`tenant_id` = seed.`tenant_id`
       AND existing_binding.`deleted` = b'0'
       AND existing_binding.`workstation_id` = seed.`workstation_id`
       AND existing_binding.`machinery_id` = mp.`machinery_id`
     GROUP BY
        seed.`tenant_id`,
        seed.`route_process_id`,
        seed.`workstation_id`,
        mp.`machinery_id`;

    SELECT COUNT(*)
      INTO v_target_workstation_machine_count
      FROM `tmp_balloon_excel_workstation_machine_seed`;

    IF v_target_workstation_machine_count <> @target_workstation_machine_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'balloon Excel workstation machine count mismatch';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_excel_workstation_machine_seed` seed
         WHERE seed.`quantity` IS NULL
            OR seed.`quantity` <= 0
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'balloon Excel workstation machine quantity invalid';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_excel_workstation_machine_seed` seed
          JOIN `mes_md_workstation_machine` existing_binding
            ON existing_binding.`id` = seed.`pre_existing_binding_id`
           AND existing_binding.`tenant_id` = seed.`tenant_id`
           AND existing_binding.`deleted` = b'0'
         WHERE existing_binding.`quantity` <> seed.`quantity`
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'workstation machine quantity conflict';
    END IF;

    INSERT INTO `mes_md_workstation_machine` (
        `workstation_id`,
        `machinery_id`,
        `quantity`,
        `remark`,
        `creator`,
        `create_time`,
        `updater`,
        `update_time`,
        `deleted`,
        `tenant_id`
    )
    SELECT
        seed.`workstation_id`,
        seed.`machinery_id`,
        seed.`quantity`,
        'Excel设备按工作站绑定',
        'balloon-excel-workstation-binding',
        NOW(),
        'balloon-excel-workstation-binding',
        NOW(),
        b'0',
        seed.`tenant_id`
      FROM `tmp_balloon_excel_workstation_machine_seed` seed
     WHERE seed.`pre_existing_binding_id` IS NULL;

    SET v_inserted_workstation_machine_count = ROW_COUNT();

    INSERT IGNORE INTO `mes_balloon_excel_workstation_machine_created_20260717` (
        `workstation_machine_id`,
        `tenant_id`,
        `workstation_id`,
        `machinery_id`
    )
    SELECT
        existing_binding.`id`,
        seed.`tenant_id`,
        seed.`workstation_id`,
        seed.`machinery_id`
      FROM `tmp_balloon_excel_workstation_machine_seed` seed
      JOIN `mes_md_workstation_machine` existing_binding
        ON existing_binding.`tenant_id` = seed.`tenant_id`
       AND existing_binding.`deleted` = b'0'
       AND existing_binding.`workstation_id` = seed.`workstation_id`
       AND existing_binding.`machinery_id` = seed.`machinery_id`
     WHERE seed.`pre_existing_binding_id` IS NULL;

    INSERT IGNORE INTO `mes_balloon_excel_workstation_binding_20260717` (
        `route_process_id`,
        `tenant_id`,
        `route_code`,
        `process_id`,
        `old_workstation_id`,
        `new_workstation_id`
    )
    SELECT
        seed.`route_process_id`,
        seed.`tenant_id`,
        seed.`route_code`,
        seed.`process_id`,
        route_process.`workstation_id`,
        seed.`workstation_id`
      FROM `tmp_balloon_excel_workstation_seed` seed
      JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = seed.`route_process_id`
       AND route_process.`tenant_id` = seed.`tenant_id`
       AND route_process.`deleted` = b'0'
     WHERE route_process.`workstation_id` IS NULL;

    UPDATE `mes_pro_route_process` route_process
      JOIN `tmp_balloon_excel_workstation_seed` seed
        ON seed.`route_process_id` = route_process.`id`
       AND seed.`tenant_id` = route_process.`tenant_id`
       SET route_process.`workstation_id` = seed.`workstation_id`,
           route_process.`updater` = 'balloon-excel-workstation-binding',
           route_process.`update_time` = NOW()
     WHERE route_process.`deleted` = b'0'
       AND route_process.`workstation_id` IS NULL;

    SET v_bound_route_process_count = ROW_COUNT();

    SELECT COUNT(*)
      INTO final_bound_route_process_count
      FROM `tmp_balloon_excel_workstation_seed` seed
      JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = seed.`route_process_id`
       AND route_process.`tenant_id` = seed.`tenant_id`
       AND route_process.`deleted` = b'0'
       AND route_process.`workstation_id` = seed.`workstation_id`;

    SELECT COUNT(*)
      INTO final_missing_workstation_count
      FROM `tmp_balloon_excel_workstation_seed` seed
      JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = seed.`route_process_id`
       AND route_process.`tenant_id` = seed.`tenant_id`
       AND route_process.`deleted` = b'0'
     WHERE route_process.`workstation_id` IS NULL
        OR route_process.`workstation_id` <> seed.`workstation_id`;

    IF final_missing_workstation_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'balloon Excel route process workstation binding incomplete';
    END IF;

    COMMIT;

    SELECT
        v_target_route_process_count AS `target_route_process_count`,
        v_inserted_workstation_count AS `inserted_workstation_count`,
        v_inserted_workstation_machine_count AS `inserted_workstation_machine_count`,
        v_bound_route_process_count AS `bound_route_process_count`,
        final_bound_route_process_count AS `final_bound_route_process_count`,
        final_missing_workstation_count AS `final_missing_workstation_count`;
END$$

DELIMITER ;

CALL `migrate_mes_balloon_excel_device_workstation_binding`();

DROP PROCEDURE IF EXISTS `migrate_mes_balloon_excel_device_workstation_binding`;

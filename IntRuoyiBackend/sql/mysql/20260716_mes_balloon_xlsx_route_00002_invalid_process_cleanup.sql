-- release-migration: allowedEnvironments=test; dependsOn=20260710_mes_route_schedule_config_unification; type=data; riskLevel=medium
-- Goal:
--   Remove the confirmed invalid 26th process from test tenant route ROUTE-XLSX-00002 before
--   20260717_mes_balloon_excel_device_workstation_binding.sql validates the 49-process baseline.
-- Safety:
--   This is a test-only data repair for tenant_id=1. It fails fast unless the target route still has
--   exactly 26 active processes and the invalid route-process row is exactly sort=26 / process=B320.
--   Existing schedule snapshots are only soft-deleted when they have no reported quantity.
-- Rollback:
--   Use mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716 to restore deleted/enabled
--   flags and previous next_process_id values if this test-only repair must be reversed.

SET @target_tenant_id = 1;
SET @target_route_hex = '524F5554452D584C53582D3030303032';
SET @target_sort = 26;
SET @target_process_code_hex = '42333230';

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_mes_balloon_xlsx_route_00002_invalid_process_cleanup`$$
CREATE PROCEDURE `migrate_mes_balloon_xlsx_route_00002_invalid_process_cleanup`()
BEGIN
    DECLARE v_route_id bigint DEFAULT NULL;
    DECLARE v_target_process_id bigint DEFAULT NULL;
    DECLARE v_target_route_process_id bigint DEFAULT NULL;
    DECLARE v_route_00002_active_process_count int DEFAULT 0;
    DECLARE v_target_route_process_count int DEFAULT 0;
    DECLARE v_previous_link_count int DEFAULT 0;
    DECLARE v_target_schedule_reported_count int DEFAULT 0;
    DECLARE final_route_00002_active_process_count int DEFAULT 0;
    DECLARE final_target_active_count int DEFAULT 0;
    DECLARE final_open_reference_count int DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*)
      INTO v_route_00002_active_process_count
      FROM `mes_pro_route_process` route_process
      JOIN `mes_pro_route` route
        ON route.`id` = route_process.`route_id`
       AND route.`tenant_id` = route_process.`tenant_id`
       AND route.`deleted` = b'0'
     WHERE route_process.`tenant_id` = @target_tenant_id
       AND route_process.`deleted` = b'0'
       AND HEX(route.`code`) = @target_route_hex;

    IF v_route_00002_active_process_count <> 26 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'balloon XLSX route 00002 invalid process cleanup route count mismatch';
    END IF;

    SELECT COUNT(*),
           MAX(route_process.`route_id`),
           MAX(route_process.`process_id`),
           MAX(route_process.`id`)
      INTO v_target_route_process_count,
           v_route_id,
           v_target_process_id,
           v_target_route_process_id
      FROM `mes_pro_route_process` route_process
      JOIN `mes_pro_route` route
        ON route.`id` = route_process.`route_id`
       AND route.`tenant_id` = route_process.`tenant_id`
       AND route.`deleted` = b'0'
      JOIN `mes_pro_process` process
        ON process.`id` = route_process.`process_id`
       AND process.`tenant_id` = route_process.`tenant_id`
       AND process.`deleted` = b'0'
     WHERE route_process.`tenant_id` = @target_tenant_id
       AND route_process.`deleted` = b'0'
       AND route_process.`sort` = @target_sort
       AND HEX(route.`code`) = @target_route_hex
       AND HEX(process.`code`) = @target_process_code_hex;

    IF v_target_route_process_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'balloon XLSX route 00002 invalid process cleanup target mismatch';
    END IF;

    SELECT COUNT(*)
      INTO v_previous_link_count
      FROM `mes_pro_route_process` previous_route_process
     WHERE previous_route_process.`tenant_id` = @target_tenant_id
       AND previous_route_process.`route_id` = v_route_id
       AND previous_route_process.`deleted` = b'0'
       AND previous_route_process.`next_process_id` = v_target_process_id;

    IF v_previous_link_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'balloon XLSX route 00002 invalid process cleanup predecessor mismatch';
    END IF;

    SELECT COUNT(*)
      INTO v_target_schedule_reported_count
      FROM `mes_pro_schedule_order_process` schedule_process
     WHERE schedule_process.`tenant_id` = @target_tenant_id
       AND schedule_process.`route_process_id` = v_target_route_process_id
       AND schedule_process.`deleted` = b'0'
       AND schedule_process.`reported_quantity` <> 0;

    IF v_target_schedule_reported_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'balloon XLSX route 00002 invalid process cleanup has reported schedule data';
    END IF;

    CREATE TABLE IF NOT EXISTS `mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716` (
        `id` bigint NOT NULL AUTO_INCREMENT,
        `record_type` varchar(64) NOT NULL,
        `source_table` varchar(128) NOT NULL,
        `source_id` bigint NOT NULL,
        `tenant_id` bigint NOT NULL,
        `route_id` bigint DEFAULT NULL,
        `route_process_id` bigint DEFAULT NULL,
        `process_id` bigint DEFAULT NULL,
        `sort` int DEFAULT NULL,
        `old_next_process_id` bigint DEFAULT NULL,
        `old_enabled` bit(1) DEFAULT NULL,
        `old_deleted` bit(1) DEFAULT NULL,
        `old_remark` varchar(500) DEFAULT NULL,
        `old_updater` varchar(64) DEFAULT NULL,
        `old_update_time` datetime DEFAULT NULL,
        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_mes_balloon_xlsx_route_00002_cleanup_source` (`source_table`, `source_id`, `record_type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='20260716 测试服 ROUTE-XLSX-00002 非法第26道工序清理备份';

    START TRANSACTION;

    INSERT IGNORE INTO `mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716` (
        `record_type`, `source_table`, `source_id`, `tenant_id`, `route_id`, `route_process_id`,
        `process_id`, `sort`, `old_next_process_id`, `old_enabled`, `old_deleted`, `old_remark`,
        `old_updater`, `old_update_time`
    )
    SELECT 'route_process',
           'mes_pro_route_process',
           target_route_process.`id`,
           target_route_process.`tenant_id`,
           target_route_process.`route_id`,
           target_route_process.`id`,
           target_route_process.`process_id`,
           target_route_process.`sort`,
           target_route_process.`next_process_id`,
           NULL,
           target_route_process.`deleted`,
           target_route_process.`remark`,
           target_route_process.`updater`,
           target_route_process.`update_time`
      FROM `mes_pro_route_process` target_route_process
     WHERE target_route_process.`id` = v_target_route_process_id
       AND target_route_process.`tenant_id` = @target_tenant_id;

    INSERT IGNORE INTO `mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716` (
        `record_type`, `source_table`, `source_id`, `tenant_id`, `route_id`, `route_process_id`,
        `process_id`, `sort`, `old_next_process_id`, `old_enabled`, `old_deleted`, `old_remark`,
        `old_updater`, `old_update_time`
    )
    SELECT 'previous_route_process',
           'mes_pro_route_process',
           previous_route_process.`id`,
           previous_route_process.`tenant_id`,
           previous_route_process.`route_id`,
           previous_route_process.`id`,
           previous_route_process.`process_id`,
           previous_route_process.`sort`,
           previous_route_process.`next_process_id`,
           NULL,
           previous_route_process.`deleted`,
           previous_route_process.`remark`,
           previous_route_process.`updater`,
           previous_route_process.`update_time`
      FROM `mes_pro_route_process` previous_route_process
     WHERE previous_route_process.`tenant_id` = @target_tenant_id
       AND previous_route_process.`route_id` = v_route_id
       AND previous_route_process.`deleted` = b'0'
       AND previous_route_process.`next_process_id` = v_target_process_id;

    INSERT IGNORE INTO `mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716` (
        `record_type`, `source_table`, `source_id`, `tenant_id`, `route_id`, `route_process_id`,
        `process_id`, `sort`, `old_next_process_id`, `old_enabled`, `old_deleted`, `old_remark`,
        `old_updater`, `old_update_time`
    )
    SELECT 'route_flow_process_config',
           'mes_pro_route_flow_process_config',
           flow_config.`id`,
           flow_config.`tenant_id`,
           flow_config.`route_id`,
           flow_config.`route_process_id`,
           NULL,
           NULL,
           NULL,
           flow_config.`enabled`,
           flow_config.`deleted`,
           flow_config.`remark`,
           flow_config.`updater`,
           flow_config.`update_time`
      FROM `mes_pro_route_flow_process_config` flow_config
     WHERE flow_config.`tenant_id` = @target_tenant_id
       AND flow_config.`route_process_id` = v_target_route_process_id
       AND flow_config.`deleted` = b'0';

    INSERT IGNORE INTO `mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716` (
        `record_type`, `source_table`, `source_id`, `tenant_id`, `route_id`, `route_process_id`,
        `process_id`, `sort`, `old_next_process_id`, `old_enabled`, `old_deleted`, `old_remark`,
        `old_updater`, `old_update_time`
    )
    SELECT 'route_schedule_config',
           'mes_pro_route_schedule_config',
           schedule_config.`id`,
           schedule_config.`tenant_id`,
           NULL,
           schedule_config.`route_process_id`,
           NULL,
           NULL,
           NULL,
           NULL,
           schedule_config.`deleted`,
           schedule_config.`remark`,
           schedule_config.`updater`,
           schedule_config.`update_time`
      FROM `mes_pro_route_schedule_config` schedule_config
     WHERE schedule_config.`tenant_id` = @target_tenant_id
       AND schedule_config.`route_process_id` = v_target_route_process_id
       AND schedule_config.`deleted` = b'0';

    INSERT IGNORE INTO `mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716` (
        `record_type`, `source_table`, `source_id`, `tenant_id`, `route_id`, `route_process_id`,
        `process_id`, `sort`, `old_next_process_id`, `old_enabled`, `old_deleted`, `old_remark`,
        `old_updater`, `old_update_time`
    )
    SELECT 'legacy_route_use_process_config',
           'mes_pro_route_use_process_config_legacy_20260709',
           legacy_config.`id`,
           legacy_config.`tenant_id`,
           legacy_config.`route_id`,
           legacy_config.`route_process_id`,
           NULL,
           NULL,
           NULL,
           legacy_config.`enabled`,
           legacy_config.`deleted`,
           legacy_config.`remark`,
           legacy_config.`updater`,
           legacy_config.`update_time`
      FROM `mes_pro_route_use_process_config_legacy_20260709` legacy_config
     WHERE legacy_config.`tenant_id` = @target_tenant_id
       AND legacy_config.`route_process_id` = v_target_route_process_id
       AND legacy_config.`deleted` = b'0';

    INSERT IGNORE INTO `mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716` (
        `record_type`, `source_table`, `source_id`, `tenant_id`, `route_id`, `route_process_id`,
        `process_id`, `sort`, `old_next_process_id`, `old_enabled`, `old_deleted`, `old_remark`,
        `old_updater`, `old_update_time`
    )
    SELECT 'schedule_order_process',
           'mes_pro_schedule_order_process',
           schedule_process.`id`,
           schedule_process.`tenant_id`,
           NULL,
           schedule_process.`route_process_id`,
           schedule_process.`process_id`,
           schedule_process.`sort`,
           NULL,
           schedule_process.`enabled`,
           schedule_process.`deleted`,
           schedule_process.`remark`,
           schedule_process.`updater`,
           schedule_process.`update_time`
      FROM `mes_pro_schedule_order_process` schedule_process
     WHERE schedule_process.`tenant_id` = @target_tenant_id
       AND schedule_process.`route_process_id` = v_target_route_process_id
       AND schedule_process.`deleted` = b'0';

    UPDATE `mes_pro_route_process` previous_route_process
       SET previous_route_process.`next_process_id` = NULL,
           previous_route_process.`updater` = 'balloon-xlsx-route-00002-cleanup',
           previous_route_process.`update_time` = NOW()
     WHERE previous_route_process.`tenant_id` = @target_tenant_id
       AND previous_route_process.`route_id` = v_route_id
       AND previous_route_process.`deleted` = b'0'
       AND previous_route_process.`next_process_id` = v_target_process_id;

    UPDATE `mes_pro_route_flow_process_config` flow_config
       SET flow_config.`deleted` = b'1',
           flow_config.`updater` = 'balloon-xlsx-route-00002-cleanup',
           flow_config.`update_time` = NOW()
     WHERE flow_config.`tenant_id` = @target_tenant_id
       AND flow_config.`route_process_id` = v_target_route_process_id
       AND flow_config.`deleted` = b'0';

    UPDATE `mes_pro_route_schedule_config` schedule_config
       SET schedule_config.`deleted` = b'1',
           schedule_config.`updater` = 'balloon-xlsx-route-00002-cleanup',
           schedule_config.`update_time` = NOW()
     WHERE schedule_config.`tenant_id` = @target_tenant_id
       AND schedule_config.`route_process_id` = v_target_route_process_id
       AND schedule_config.`deleted` = b'0';

    UPDATE `mes_pro_route_use_process_config_legacy_20260709` legacy_config
       SET legacy_config.`deleted` = b'1',
           legacy_config.`updater` = 'balloon-xlsx-route-00002-cleanup',
           legacy_config.`update_time` = NOW()
     WHERE legacy_config.`tenant_id` = @target_tenant_id
       AND legacy_config.`route_process_id` = v_target_route_process_id
       AND legacy_config.`deleted` = b'0';

    UPDATE `mes_pro_schedule_order_process` schedule_process
       SET schedule_process.`enabled` = b'0',
           schedule_process.`deleted` = b'1',
           schedule_process.`updater` = 'balloon-xlsx-route-00002-cleanup',
           schedule_process.`update_time` = NOW()
     WHERE schedule_process.`tenant_id` = @target_tenant_id
       AND schedule_process.`route_process_id` = v_target_route_process_id
       AND schedule_process.`deleted` = b'0';

    UPDATE `mes_pro_route_process` target_route_process
       SET target_route_process.`deleted` = b'1',
           target_route_process.`next_process_id` = NULL,
           target_route_process.`updater` = 'balloon-xlsx-route-00002-cleanup',
           target_route_process.`update_time` = NOW()
     WHERE target_route_process.`tenant_id` = @target_tenant_id
       AND target_route_process.`id` = v_target_route_process_id
       AND target_route_process.`deleted` = b'0';

    SELECT COUNT(*)
      INTO final_target_active_count
      FROM `mes_pro_route_process` target_route_process
     WHERE target_route_process.`tenant_id` = @target_tenant_id
       AND target_route_process.`id` = v_target_route_process_id
       AND target_route_process.`deleted` = b'0';

    IF final_target_active_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'balloon XLSX route 00002 invalid process cleanup target still active';
    END IF;

    SELECT COUNT(*)
      INTO final_route_00002_active_process_count
      FROM `mes_pro_route_process` route_process
      JOIN `mes_pro_route` route
        ON route.`id` = route_process.`route_id`
       AND route.`tenant_id` = route_process.`tenant_id`
       AND route.`deleted` = b'0'
     WHERE route_process.`tenant_id` = @target_tenant_id
       AND route_process.`deleted` = b'0'
       AND HEX(route.`code`) = @target_route_hex;

    IF final_route_00002_active_process_count <> 25 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'balloon XLSX route 00002 invalid process cleanup final route count mismatch';
    END IF;

    SELECT (
        (SELECT COUNT(*) FROM `mes_pro_route_flow_process_config`
          WHERE `tenant_id` = @target_tenant_id
            AND `route_process_id` = v_target_route_process_id
            AND `deleted` = b'0')
        + (SELECT COUNT(*) FROM `mes_pro_route_schedule_config`
          WHERE `tenant_id` = @target_tenant_id
            AND `route_process_id` = v_target_route_process_id
            AND `deleted` = b'0')
        + (SELECT COUNT(*) FROM `mes_pro_route_use_process_config_legacy_20260709`
          WHERE `tenant_id` = @target_tenant_id
            AND `route_process_id` = v_target_route_process_id
            AND `deleted` = b'0')
        + (SELECT COUNT(*) FROM `mes_pro_schedule_order_process`
          WHERE `tenant_id` = @target_tenant_id
            AND `route_process_id` = v_target_route_process_id
            AND `deleted` = b'0')
    )
      INTO final_open_reference_count;

    IF final_open_reference_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'balloon XLSX route 00002 invalid process cleanup active references remain';
    END IF;

    COMMIT;

    SELECT
        v_target_route_process_id AS `route_process_id`,
        v_route_00002_active_process_count AS `before_route_00002_active_process_count`,
        final_route_00002_active_process_count AS `after_route_00002_active_process_count`,
        final_open_reference_count AS `active_reference_count`;
END$$

DELIMITER ;

CALL `migrate_mes_balloon_xlsx_route_00002_invalid_process_cleanup`();

DROP PROCEDURE IF EXISTS `migrate_mes_balloon_xlsx_route_00002_invalid_process_cleanup`;

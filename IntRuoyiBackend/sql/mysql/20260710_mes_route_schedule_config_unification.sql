-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_mes_smart_scheduling_t1_schema; type=data; riskLevel=high
-- MES 路线工序排产配置全局统一：每个租户、路线版本、路线工序只保留一条有效通用配置。
-- 回滚说明：本迁移会重定向在制快照并软删除产品级配置，执行前必须完成数据库备份；回滚时恢复备份，不允许按历史产品配置自动反推。
-- 统一口径：item_id IS NULL 为通用配置，item_id IS NOT NULL 为待软删除的历史产品配置。
-- 有效唯一标记表达式：IF(`deleted` = b'0', 1, NULL)。

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_mes_route_schedule_config_unification`$$
CREATE PROCEDURE `migrate_mes_route_schedule_config_unification`()
BEGIN
    DECLARE invalid_generic_group_count bigint DEFAULT 0;
    DECLARE invalid_wip_route_context_count bigint DEFAULT 0;
    DECLARE invalid_wip_deleted_route_process_count bigint DEFAULT 0;
    DECLARE invalid_wip_canonical_config_count bigint DEFAULT 0;
    DECLARE active_product_config_count bigint DEFAULT 0;
    DECLARE active_orphan_config_count bigint DEFAULT 0;
    DECLARE dangling_config_reference_count bigint DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*)
      INTO invalid_generic_group_count
      FROM (
          SELECT schedule_config.`tenant_id`,
                 schedule_config.`route_version_id`,
                 schedule_config.`route_process_id`
            FROM `mes_pro_route_schedule_config` schedule_config
            JOIN `mes_pro_route_version` route_version
              ON route_version.`id` = schedule_config.`route_version_id`
             AND route_version.`tenant_id` = schedule_config.`tenant_id`
             AND route_version.`deleted` = b'0'
            JOIN `mes_pro_route_process` route_process
              ON route_process.`id` = schedule_config.`route_process_id`
             AND route_process.`tenant_id` = schedule_config.`tenant_id`
             AND route_process.`deleted` = b'0'
             AND route_process.`route_id` = route_version.`route_id`
           WHERE schedule_config.`deleted` = b'0'
           GROUP BY schedule_config.`tenant_id`,
                    schedule_config.`route_version_id`,
                    schedule_config.`route_process_id`
          HAVING SUM(CASE WHEN schedule_config.`item_id` IS NULL THEN 1 ELSE 0 END) <> 1
      ) invalid_group;

    IF invalid_generic_group_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'route schedule config missing exactly one generic config';
    END IF;

    SELECT COUNT(*)
      INTO invalid_wip_route_context_count
      FROM `mes_pro_schedule_order_process` process_snapshot
      JOIN `mes_pro_schedule_order` schedule_order
        ON schedule_order.`id` = process_snapshot.`schedule_order_id`
       AND schedule_order.`tenant_id` = process_snapshot.`tenant_id`
     WHERE process_snapshot.`deleted` = b'0'
       AND process_snapshot.`enabled` = b'1'
       AND (process_snapshot.`progress_percent` IS NULL OR process_snapshot.`progress_percent` < 100)
       AND schedule_order.`deleted` = b'0'
       AND schedule_order.`status` IN (0, 1, 2)
       AND schedule_order.`frozen` = b'0'
       AND schedule_order.`manual_finished` = b'0'
       AND (schedule_order.`route_id` IS NULL OR process_snapshot.`route_process_id` IS NULL);

    IF invalid_wip_route_context_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'active WIP route process missing route context';
    END IF;

    SELECT COUNT(*)
      INTO invalid_wip_canonical_config_count
      FROM `mes_pro_schedule_order_process` process_snapshot
      JOIN `mes_pro_schedule_order` schedule_order
        ON schedule_order.`id` = process_snapshot.`schedule_order_id`
       AND schedule_order.`tenant_id` = process_snapshot.`tenant_id`
      JOIN `mes_pro_route_version` route_version
        ON route_version.`id` = COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`)
       AND route_version.`tenant_id` = process_snapshot.`tenant_id`
       AND route_version.`deleted` = b'0'
      JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = process_snapshot.`route_process_id`
       AND route_process.`tenant_id` = process_snapshot.`tenant_id`
       AND route_process.`deleted` = b'0'
       AND route_process.`route_id` = route_version.`route_id`
     WHERE process_snapshot.`deleted` = b'0'
       AND process_snapshot.`enabled` = b'1'
       AND (process_snapshot.`progress_percent` IS NULL OR process_snapshot.`progress_percent` < 100)
       AND schedule_order.`deleted` = b'0'
       AND schedule_order.`status` IN (0, 1, 2)
       AND schedule_order.`frozen` = b'0'
       AND schedule_order.`manual_finished` = b'0'
       AND (
           CASE
               WHEN COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`) IS NOT NULL
                   THEN (
                       SELECT COUNT(*)
                         FROM `mes_pro_route_schedule_config` canonical_config
                        WHERE canonical_config.`tenant_id` = process_snapshot.`tenant_id`
                          AND canonical_config.`route_version_id` =
                              COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`)
                          AND canonical_config.`route_process_id` = process_snapshot.`route_process_id`
                          AND canonical_config.`deleted` = b'0'
                          AND canonical_config.`item_id` IS NULL
                   )
               ELSE (
                   SELECT COUNT(*)
                     FROM `mes_pro_route_schedule_config` canonical_config
                    WHERE canonical_config.`tenant_id` = process_snapshot.`tenant_id`
                      AND canonical_config.`route_process_id` = process_snapshot.`route_process_id`
                      AND canonical_config.`deleted` = b'0'
                      AND canonical_config.`item_id` IS NULL
               )
           END
       ) <> 1;

    IF invalid_wip_canonical_config_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'active WIP route process missing unique generic schedule config';
    END IF;

    SET @schema_name := DATABASE();

    SET @sql := (
        SELECT IF(COUNT(*) > 0,
            'ALTER TABLE `mes_pro_route_schedule_config` DROP INDEX `uk_mes_pro_route_schedule_config_item_process`',
            'SELECT 1')
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @schema_name
           AND TABLE_NAME = 'mes_pro_route_schedule_config'
           AND INDEX_NAME = 'uk_mes_pro_route_schedule_config_item_process'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SET @sql := (
        SELECT IF(COUNT(*) > 0,
            'ALTER TABLE `mes_pro_route_schedule_config` DROP INDEX `uk_mes_pro_route_schedule_config_process`',
            'SELECT 1')
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @schema_name
           AND TABLE_NAME = 'mes_pro_route_schedule_config'
           AND INDEX_NAME = 'uk_mes_pro_route_schedule_config_process'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    START TRANSACTION;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_schedule_orphan_config`;
    CREATE TEMPORARY TABLE `tmp_mes_route_schedule_orphan_config` (
        `orphan_config_id` bigint NOT NULL,
        PRIMARY KEY (`orphan_config_id`)
    ) ENGINE=InnoDB;

    INSERT INTO `tmp_mes_route_schedule_orphan_config` (`orphan_config_id`)
    SELECT schedule_config.`id`
      FROM `mes_pro_route_schedule_config` schedule_config
      LEFT JOIN `mes_pro_route_version` route_version
        ON route_version.`id` = schedule_config.`route_version_id`
       AND route_version.`tenant_id` = schedule_config.`tenant_id`
       AND route_version.`deleted` = b'0'
      LEFT JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = schedule_config.`route_process_id`
       AND route_process.`tenant_id` = schedule_config.`tenant_id`
     WHERE schedule_config.`deleted` = b'0'
       AND (
           route_version.`id` IS NULL
           OR route_process.`id` IS NULL
           OR route_process.`deleted` = b'1'
           OR route_process.`route_id` <> route_version.`route_id`
       );

    UPDATE `mes_pro_schedule_order_process` process_snapshot
    JOIN `tmp_mes_route_schedule_orphan_config` orphan_config
      ON orphan_config.`orphan_config_id` = process_snapshot.`route_schedule_config_id`
       SET process_snapshot.`route_schedule_config_id` = NULL;

    UPDATE `mes_pro_schedule_order_process` process_snapshot
    JOIN `mes_pro_schedule_order` schedule_order
      ON schedule_order.`id` = process_snapshot.`schedule_order_id`
     AND schedule_order.`tenant_id` = process_snapshot.`tenant_id`
    LEFT JOIN `mes_pro_route_version` route_version
      ON route_version.`id` = COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`)
     AND route_version.`tenant_id` = process_snapshot.`tenant_id`
     AND route_version.`deleted` = b'0'
    LEFT JOIN `mes_pro_route_process` route_process
      ON route_process.`id` = process_snapshot.`route_process_id`
     AND route_process.`tenant_id` = process_snapshot.`tenant_id`
       SET process_snapshot.`enabled` = b'0',
           process_snapshot.`route_schedule_config_id` = NULL
     WHERE process_snapshot.`deleted` = b'0'
       AND process_snapshot.`enabled` = b'1'
       AND (process_snapshot.`progress_percent` IS NULL OR process_snapshot.`progress_percent` < 100)
       AND schedule_order.`deleted` = b'0'
       AND schedule_order.`status` IN (0, 1, 2)
       AND schedule_order.`frozen` = b'0'
       AND schedule_order.`manual_finished` = b'0'
       AND (
           route_process.`id` IS NULL
           OR route_process.`deleted` = b'1'
           OR (
               route_version.`id` IS NOT NULL
               AND route_process.`route_id` <> route_version.`route_id`
           )
       );

    UPDATE `mes_pro_route_schedule_config` schedule_config
    JOIN `tmp_mes_route_schedule_orphan_config` orphan_config
      ON orphan_config.`orphan_config_id` = schedule_config.`id`
       SET schedule_config.`deleted` = b'1',
           schedule_config.`update_time` = CURRENT_TIMESTAMP;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_schedule_canonical_config`;
    CREATE TEMPORARY TABLE `tmp_mes_route_schedule_canonical_config` (
        `tenant_id` bigint NOT NULL,
        `route_version_id` bigint NOT NULL,
        `route_process_id` bigint NOT NULL,
        `canonical_config_id` bigint NOT NULL,
        `capacity_mode` varchar(32) NOT NULL,
        `hourly_capacity` decimal(18,6) NULL,
        `infinite_duration_quantity_factor` decimal(18,6) NULL,
        `infinite_duration_base_minutes` decimal(18,6) NULL,
        `night_shift_enabled` bit(1) NULL,
        `calendar_rule_id` bigint NULL,
        PRIMARY KEY (`tenant_id`, `route_version_id`, `route_process_id`)
    ) ENGINE=InnoDB;

    INSERT INTO `tmp_mes_route_schedule_canonical_config` (
        `tenant_id`, `route_version_id`, `route_process_id`, `canonical_config_id`,
        `capacity_mode`, `hourly_capacity`, `infinite_duration_quantity_factor`,
        `infinite_duration_base_minutes`, `night_shift_enabled`, `calendar_rule_id`
    )
    SELECT schedule_config.`tenant_id`,
           schedule_config.`route_version_id`,
           schedule_config.`route_process_id`,
           schedule_config.`id`,
           schedule_config.`capacity_mode`,
           schedule_config.`hourly_capacity`,
           schedule_config.`infinite_duration_quantity_factor`,
           schedule_config.`infinite_duration_base_minutes`,
           schedule_config.`night_shift_enabled`,
           schedule_config.`calendar_rule_id`
      FROM `mes_pro_route_schedule_config` schedule_config
      JOIN `mes_pro_route_version` route_version
        ON route_version.`id` = schedule_config.`route_version_id`
       AND route_version.`tenant_id` = schedule_config.`tenant_id`
       AND route_version.`deleted` = b'0'
      JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = schedule_config.`route_process_id`
       AND route_process.`tenant_id` = schedule_config.`tenant_id`
       AND route_process.`deleted` = b'0'
       AND route_process.`route_id` = route_version.`route_id`
     WHERE schedule_config.`deleted` = b'0'
       AND schedule_config.`item_id` IS NULL;

    UPDATE `mes_pro_schedule_order_process` process_snapshot
    JOIN `mes_pro_route_schedule_config` referenced_config
      ON referenced_config.`id` = process_snapshot.`route_schedule_config_id`
     AND referenced_config.`tenant_id` = process_snapshot.`tenant_id`
    JOIN `tmp_mes_route_schedule_canonical_config` canonical
      ON canonical.`tenant_id` = referenced_config.`tenant_id`
     AND canonical.`route_version_id` = referenced_config.`route_version_id`
     AND canonical.`route_process_id` = referenced_config.`route_process_id`
       SET process_snapshot.`route_version_id` = canonical.`route_version_id`,
           process_snapshot.`route_schedule_config_id` = canonical.`canonical_config_id`
     WHERE referenced_config.`deleted` = b'1'
        OR referenced_config.`item_id` IS NOT NULL;

    UPDATE `mes_pro_schedule_order_process` process_snapshot
    JOIN `mes_pro_schedule_order` schedule_order
      ON schedule_order.`id` = process_snapshot.`schedule_order_id`
     AND schedule_order.`tenant_id` = process_snapshot.`tenant_id`
     AND schedule_order.`deleted` = b'0'
    JOIN `tmp_mes_route_schedule_canonical_config` canonical
      ON canonical.`tenant_id` = process_snapshot.`tenant_id`
     AND canonical.`route_version_id` = COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`)
     AND canonical.`route_process_id` = process_snapshot.`route_process_id`
       SET process_snapshot.`route_version_id` = canonical.`route_version_id`,
           process_snapshot.`route_schedule_config_id` = canonical.`canonical_config_id`;

    UPDATE `mes_pro_schedule_order_process` process_snapshot
    LEFT JOIN `mes_pro_schedule_order` schedule_order
      ON schedule_order.`id` = process_snapshot.`schedule_order_id`
     AND schedule_order.`tenant_id` = process_snapshot.`tenant_id`
    JOIN (
        SELECT `tenant_id`, `route_process_id`,
               MIN(`route_version_id`) AS `route_version_id`,
               MIN(`canonical_config_id`) AS `canonical_config_id`
          FROM `tmp_mes_route_schedule_canonical_config`
         GROUP BY `tenant_id`, `route_process_id`
        HAVING COUNT(*) = 1
    ) unique_canonical
      ON unique_canonical.`tenant_id` = process_snapshot.`tenant_id`
     AND unique_canonical.`route_process_id` = process_snapshot.`route_process_id`
       SET process_snapshot.`route_version_id` = unique_canonical.`route_version_id`,
           process_snapshot.`route_schedule_config_id` = unique_canonical.`canonical_config_id`
     WHERE process_snapshot.`route_version_id` IS NULL
       AND (schedule_order.`id` IS NULL OR schedule_order.`route_version_id` IS NULL);

    UPDATE `mes_pro_schedule_order_process` process_snapshot
    JOIN `mes_pro_schedule_order` schedule_order
      ON schedule_order.`id` = process_snapshot.`schedule_order_id`
     AND schedule_order.`tenant_id` = process_snapshot.`tenant_id`
     AND schedule_order.`deleted` = b'0'
    JOIN `tmp_mes_route_schedule_canonical_config` canonical
      ON canonical.`tenant_id` = process_snapshot.`tenant_id`
     AND canonical.`route_version_id` = COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`)
     AND canonical.`route_process_id` = process_snapshot.`route_process_id`
       SET process_snapshot.`capacity_mode` = canonical.`capacity_mode`,
           process_snapshot.`hourly_capacity_total` = canonical.`hourly_capacity`,
           process_snapshot.`infinite_duration_quantity_factor` = canonical.`infinite_duration_quantity_factor`,
           process_snapshot.`infinite_duration_base_minutes` = canonical.`infinite_duration_base_minutes`,
           process_snapshot.`shift_capacity_total` =
               CASE
                   WHEN canonical.`capacity_mode` = 'FINITE_HOURLY'
                       THEN canonical.`hourly_capacity` * COALESCE(NULLIF(process_snapshot.`shift_hours`, 0), 10.5)
                   ELSE process_snapshot.`shift_capacity_total`
               END,
           process_snapshot.`night_shift_enabled` = canonical.`night_shift_enabled`,
           process_snapshot.`calendar_rule_id` = canonical.`calendar_rule_id`
     WHERE process_snapshot.`deleted` = b'0'
       AND process_snapshot.`enabled` = b'1'
       AND (process_snapshot.`progress_percent` IS NULL OR process_snapshot.`progress_percent` < 100)
       AND schedule_order.`frozen` = b'0'
       AND schedule_order.`manual_finished` = b'0';

    UPDATE `mes_pro_route_schedule_config` product_config
       SET product_config.`deleted` = b'1',
           product_config.`update_time` = CURRENT_TIMESTAMP
     WHERE product_config.`deleted` = b'0'
       AND product_config.`item_id` IS NOT NULL;

    SELECT COUNT(*)
      INTO active_product_config_count
      FROM `mes_pro_route_schedule_config`
     WHERE `deleted` = b'0'
       AND `item_id` IS NOT NULL;

    IF active_product_config_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'active product route schedule config remains after migration';
    END IF;

    SELECT COUNT(*)
      INTO active_orphan_config_count
      FROM `mes_pro_route_schedule_config` schedule_config
      LEFT JOIN `mes_pro_route_version` route_version
        ON route_version.`id` = schedule_config.`route_version_id`
       AND route_version.`tenant_id` = schedule_config.`tenant_id`
       AND route_version.`deleted` = b'0'
      LEFT JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = schedule_config.`route_process_id`
       AND route_process.`tenant_id` = schedule_config.`tenant_id`
     WHERE schedule_config.`deleted` = b'0'
       AND (
           route_version.`id` IS NULL
           OR route_process.`id` IS NULL
           OR route_process.`deleted` = b'1'
           OR route_process.`route_id` <> route_version.`route_id`
       );

    IF active_orphan_config_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'active route schedule config references deleted route process';
    END IF;

    SELECT COUNT(*)
      INTO invalid_wip_deleted_route_process_count
      FROM `mes_pro_schedule_order_process` process_snapshot
      JOIN `mes_pro_schedule_order` schedule_order
        ON schedule_order.`id` = process_snapshot.`schedule_order_id`
       AND schedule_order.`tenant_id` = process_snapshot.`tenant_id`
      LEFT JOIN `mes_pro_route_version` route_version
        ON route_version.`id` = COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`)
       AND route_version.`tenant_id` = process_snapshot.`tenant_id`
       AND route_version.`deleted` = b'0'
      LEFT JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = process_snapshot.`route_process_id`
       AND route_process.`tenant_id` = process_snapshot.`tenant_id`
     WHERE process_snapshot.`deleted` = b'0'
       AND process_snapshot.`enabled` = b'1'
       AND (process_snapshot.`progress_percent` IS NULL OR process_snapshot.`progress_percent` < 100)
       AND schedule_order.`deleted` = b'0'
       AND schedule_order.`status` IN (0, 1, 2)
       AND schedule_order.`frozen` = b'0'
       AND schedule_order.`manual_finished` = b'0'
       AND (
           route_process.`id` IS NULL
           OR route_process.`deleted` = b'1'
           OR (
               route_version.`id` IS NOT NULL
               AND route_process.`route_id` <> route_version.`route_id`
           )
       );

    IF invalid_wip_deleted_route_process_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'active WIP still references deleted route process after migration';
    END IF;

    SELECT COUNT(*)
      INTO dangling_config_reference_count
      FROM `mes_pro_schedule_order_process` process_snapshot
     JOIN `mes_pro_route_schedule_config` referenced_config
        ON referenced_config.`id` = process_snapshot.`route_schedule_config_id`
       AND referenced_config.`tenant_id` = process_snapshot.`tenant_id`
      WHERE process_snapshot.`deleted` = b'0'
        AND referenced_config.`item_id` IS NOT NULL;

    IF dangling_config_reference_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'schedule order process still references retired product config';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_schedule_canonical_config`;
    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_schedule_orphan_config`;
    COMMIT;
END$$

DELIMITER ;

CALL `migrate_mes_route_schedule_config_unification`();
DROP PROCEDURE IF EXISTS `migrate_mes_route_schedule_config_unification`;

SET @schema_name := DATABASE();

SET @sql := (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE `mes_pro_route_schedule_config` DROP INDEX `uk_mes_pro_route_schedule_config_item_process`',
        'SELECT 1')
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'mes_pro_route_schedule_config'
       AND INDEX_NAME = 'uk_mes_pro_route_schedule_config_item_process'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE `mes_pro_route_schedule_config` DROP INDEX `uk_mes_pro_route_schedule_config_process`',
        'SELECT 1')
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'mes_pro_route_schedule_config'
       AND INDEX_NAME = 'uk_mes_pro_route_schedule_config_process'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `mes_pro_route_schedule_config` ADD COLUMN `active_unique_flag` tinyint GENERATED ALWAYS AS (IF(`deleted` = b''0'', 1, NULL)) STORED AFTER `deleted`',
        'SELECT 1')
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'mes_pro_route_schedule_config'
       AND COLUMN_NAME = 'active_unique_flag'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `mes_pro_route_schedule_config` ADD UNIQUE INDEX `uk_mes_pro_route_schedule_config_active_process` (`tenant_id`, `route_version_id`, `route_process_id`, `active_unique_flag`) USING BTREE',
        'SELECT 1')
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @schema_name
       AND TABLE_NAME = 'mes_pro_route_schedule_config'
       AND INDEX_NAME = 'uk_mes_pro_route_schedule_config_active_process'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DELIMITER $$

DROP PROCEDURE IF EXISTS `verify_mes_route_schedule_config_unification`$$
CREATE PROCEDURE `verify_mes_route_schedule_config_unification`()
BEGIN
    DECLARE invalid_active_group_count bigint DEFAULT 0;
    DECLARE active_orphan_config_count bigint DEFAULT 0;

    SELECT COUNT(*)
      INTO invalid_active_group_count
      FROM (
          SELECT `tenant_id`, `route_version_id`, `route_process_id`
            FROM `mes_pro_route_schedule_config`
           WHERE `deleted` = b'0'
           GROUP BY `tenant_id`, `route_version_id`, `route_process_id`
          HAVING COUNT(*) <> 1 OR SUM(CASE WHEN `item_id` IS NULL THEN 1 ELSE 0 END) <> 1
      ) invalid_group;

    IF invalid_active_group_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'route schedule config active uniqueness verification failed';
    END IF;

    SELECT COUNT(*)
      INTO active_orphan_config_count
      FROM `mes_pro_route_schedule_config` schedule_config
      LEFT JOIN `mes_pro_route_version` route_version
        ON route_version.`id` = schedule_config.`route_version_id`
       AND route_version.`tenant_id` = schedule_config.`tenant_id`
       AND route_version.`deleted` = b'0'
      LEFT JOIN `mes_pro_route_process` route_process
        ON route_process.`id` = schedule_config.`route_process_id`
       AND route_process.`tenant_id` = schedule_config.`tenant_id`
     WHERE schedule_config.`deleted` = b'0'
       AND (
           route_version.`id` IS NULL
           OR route_process.`id` IS NULL
           OR route_process.`deleted` = b'1'
           OR route_process.`route_id` <> route_version.`route_id`
       );

    IF active_orphan_config_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'active route schedule config references deleted route process';
    END IF;
END$$

DELIMITER ;

CALL `verify_mes_route_schedule_config_unification`();
DROP PROCEDURE IF EXISTS `verify_mes_route_schedule_config_unification`;

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260808_mes_active_order_release_application; type=schema; riskLevel=medium
-- MES 生产组长活跃订单：增加可持久化的人工排序字段
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_pp_active_order_manual_sort_20260809;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_active_order_manual_sort_20260809()
BEGIN
    DECLARE v_table_count int DEFAULT 0;
    DECLARE v_column_count int DEFAULT 0;
    DECLARE v_null_count bigint DEFAULT 0;
    DECLARE v_non_null_count bigint DEFAULT 0;
    DECLARE v_data_type varchar(64) DEFAULT NULL;
    DECLARE v_is_nullable varchar(3) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_active_order_manual_sort_20260809`;
        RESIGNAL;
    END;

    SELECT COUNT(*)
      INTO v_table_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order';
    IF v_table_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_active_order';
    END IF;

    SELECT COUNT(*)
      INTO v_column_count
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND column_name = 'sort_order';
    IF v_column_count = 0 THEN
        ALTER TABLE `mes_pro_process_pool_active_order`
            ADD COLUMN `sort_order` bigint NULL COMMENT '生产组长活跃订单人工排序' AFTER `joined_at`;
    END IF;

    SELECT `data_type`, `is_nullable`
      INTO v_data_type, v_is_nullable
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND column_name = 'sort_order';
    IF v_data_type <> 'bigint' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Active order sort_order column contract mismatch';
    END IF;

    SELECT SUM(CASE WHEN `sort_order` IS NULL THEN 1 ELSE 0 END),
           SUM(CASE WHEN `sort_order` IS NOT NULL THEN 1 ELSE 0 END)
      INTO v_null_count, v_non_null_count
      FROM `mes_pro_process_pool_active_order`;
    SET v_null_count = COALESCE(v_null_count, 0);
    SET v_non_null_count = COALESCE(v_non_null_count, 0);
    IF v_null_count > 0 AND v_non_null_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Active order sort_order partial backfill requires manual review';
    END IF;

    IF v_null_count > 0 THEN
        DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_active_order_manual_sort_20260809`;
        CREATE TEMPORARY TABLE `tmp_mes_pp_active_order_manual_sort_20260809` (
            `id` bigint NOT NULL,
            `sort_order` bigint NOT NULL,
            PRIMARY KEY (`id`)
        ) ENGINE=InnoDB;

        INSERT INTO `tmp_mes_pp_active_order_manual_sort_20260809` (`id`, `sort_order`)
        SELECT `id`,
               ROW_NUMBER() OVER (
                   PARTITION BY `tenant_id`, `leader_user_id`
                   ORDER BY `joined_at` ASC, `id` ASC
               ) AS `sort_order`
          FROM `mes_pro_process_pool_active_order`;

        UPDATE `mes_pro_process_pool_active_order` AS `target`
        JOIN `tmp_mes_pp_active_order_manual_sort_20260809` AS `ranked`
          ON `ranked`.`id` = `target`.`id`
        SET `target`.`sort_order` = `ranked`.`sort_order`
        WHERE `target`.`sort_order` IS NULL;

        DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_active_order_manual_sort_20260809`;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `mes_pro_process_pool_active_order`
         WHERE `sort_order` IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Active order sort_order backfill failed';
    END IF;

    ALTER TABLE `mes_pro_process_pool_active_order`
        MODIFY COLUMN `sort_order` bigint NOT NULL COMMENT '生产组长活跃订单人工排序';

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_active_order'
           AND index_name = 'idx_mes_pp_active_order_manual_sort'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_active_order`
            ADD KEY `idx_mes_pp_active_order_manual_sort`
                (`tenant_id`, `leader_user_id`, `active_status`, `sort_order`, `id`, `deleted`);
    END IF;

    SELECT `data_type`, `is_nullable`
      INTO v_data_type, v_is_nullable
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND column_name = 'sort_order';
    IF v_data_type <> 'bigint' OR v_is_nullable <> 'NO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Active order sort_order column contract mismatch';
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_active_order_manual_sort_20260809();

DROP PROCEDURE IF EXISTS ensure_mes_pp_active_order_manual_sort_20260809;

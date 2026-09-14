-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_process_pool_ac_m20_pqc_review_closure,20260805_mes_pqc_process_inspection_aggregate_detail; type=schema; riskLevel=medium
-- AC-M21 runtime closure: reconcile an aggregate table that AC-M20 may have created before the full AC-M21 schema.

DROP PROCEDURE IF EXISTS close_mes_pqc_process_inspection_aggregate_schema;
DELIMITER $$
CREATE PROCEDURE close_mes_pqc_process_inspection_aggregate_schema()
BEGIN
  DECLARE v_missing_required_source_count int DEFAULT 0;
  DECLARE v_duplicate_aggregate_count int DEFAULT 0;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pqc_process_inspection_aggregate_detail must exist before runtime closure';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND column_name = 'active_order_id'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      ADD COLUMN `active_order_id` bigint DEFAULT NULL COMMENT '活跃订单ID' AFTER `pqc_task_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND column_name = 'route_version_id'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      ADD COLUMN `route_version_id` bigint DEFAULT NULL COMMENT '工艺路线版本ID' AFTER `route_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND column_name = 'actual_inspection_quantity'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      ADD COLUMN `actual_inspection_quantity` int DEFAULT NULL COMMENT '实际检验数量' AFTER `round_no`;
  END IF;

  UPDATE `mes_pqc_process_inspection_aggregate_detail` `aggregate_detail`
    JOIN `mes_pqc_inspection_task` `task`
      ON `task`.`tenant_id` = `aggregate_detail`.`tenant_id`
     AND `task`.`id` = `aggregate_detail`.`pqc_task_id`
     AND `task`.`deleted` = b'0'
     SET `aggregate_detail`.`active_order_id` =
           COALESCE(`aggregate_detail`.`active_order_id`, `task`.`active_order_id`),
         `aggregate_detail`.`route_version_id` =
           COALESCE(`aggregate_detail`.`route_version_id`, `task`.`route_version_id`),
         `aggregate_detail`.`actual_inspection_quantity` = `task`.`actual_inspection_quantity`
   WHERE `aggregate_detail`.`deleted` = b'0'
     AND (
       `aggregate_detail`.`active_order_id` IS NULL
       OR `aggregate_detail`.`route_version_id` IS NULL
       OR `aggregate_detail`.`actual_inspection_quantity` IS NULL
     );

  SELECT COUNT(1)
    INTO v_missing_required_source_count
    FROM `mes_pqc_process_inspection_aggregate_detail`
   WHERE `deleted` = b'0'
     AND (
       `route_version_id` IS NULL
       OR `actual_inspection_quantity` IS NULL
     );
  IF v_missing_required_source_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'aggregate detail rows require formal PQC task route version and actual inspection quantity';
  END IF;

  ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
    MODIFY COLUMN `route_version_id` bigint NOT NULL COMMENT '工艺路线版本ID',
    MODIFY COLUMN `actual_inspection_quantity` int NOT NULL COMMENT '实际检验数量';

  SELECT COUNT(1)
    INTO v_duplicate_aggregate_count
    FROM (
      SELECT `tenant_id`, `event_id`, `source_piece_detail_id`, `deleted`
        FROM `mes_pqc_process_inspection_aggregate_detail`
       GROUP BY `tenant_id`, `event_id`, `source_piece_detail_id`, `deleted`
      HAVING COUNT(1) > 1
    ) `duplicate_aggregate`;
  IF v_duplicate_aggregate_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'aggregate detail rows contain duplicate event and source piece identities';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'uk_mes_pqc_process_inspection_aggregate'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      ADD UNIQUE KEY `uk_mes_pqc_process_inspection_aggregate`
        (`tenant_id`, `event_id`, `source_piece_detail_id`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'idx_mes_pqc_process_inspection_review'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      ADD KEY `idx_mes_pqc_process_inspection_review` (`tenant_id`, `review_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'idx_mes_pqc_process_inspection_task'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      ADD KEY `idx_mes_pqc_process_inspection_task` (`tenant_id`, `pqc_task_id`, `sample_no`);
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'idx_mes_pqc_process_inspection_submit_event'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      ADD KEY `idx_mes_pqc_process_inspection_submit_event` (`tenant_id`, `production_submit_event_id`);
  END IF;
END$$
DELIMITER ;

CALL close_mes_pqc_process_inspection_aggregate_schema();

DROP PROCEDURE IF EXISTS close_mes_pqc_process_inspection_aggregate_schema;

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task,20260802_mes_process_pool_active_order_process_snapshot,20260802_mes_qa_inspection_regulation; type=schema; riskLevel=medium
-- PQC task identity closure: historical pending tasks must carry formal route process and process identity.

DROP PROCEDURE IF EXISTS close_mes_pqc_task_process_identity;
DELIMITER $$
CREATE PROCEDURE close_mes_pqc_task_process_identity()
BEGIN
  DECLARE v_missing_formal_source_count int DEFAULT 0;
  DECLARE v_duplicate_identity_count int DEFAULT 0;
  DECLARE v_unresolved_identity_count int DEFAULT 0;
  DECLARE v_mismatched_identity_count int DEFAULT 0;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC task process identity closure requires mes_pqc_inspection_task';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order_process_snapshot'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC task process identity closure requires active order process snapshots';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation'
  ) OR NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC task process identity closure requires QA regulation and version tables';
  END IF;

  SELECT COUNT(1)
    INTO v_missing_formal_source_count
    FROM `mes_pqc_inspection_task` `task`
    LEFT JOIN `mes_qa_inspection_regulation_version` `version`
      ON `version`.`tenant_id` = `task`.`tenant_id`
     AND `version`.`id` = `task`.`regulation_version_id`
     AND `version`.`deleted` = b'0'
    LEFT JOIN `mes_qa_inspection_regulation` `regulation`
      ON `regulation`.`tenant_id` = `task`.`tenant_id`
     AND `regulation`.`id` = `version`.`regulation_id`
     AND `regulation`.`route_id` = `task`.`route_id`
     AND `regulation`.`route_version_id` = `task`.`route_version_id`
     AND `regulation`.`deleted` = b'0'
    LEFT JOIN `mes_pro_process_pool_active_order_process_snapshot` `snapshot`
      ON `snapshot`.`tenant_id` = `task`.`tenant_id`
     AND `snapshot`.`active_order_id` = `task`.`active_order_id`
     AND `snapshot`.`work_order_id` = `task`.`work_order_id`
     AND `snapshot`.`route_id` = `task`.`route_id`
     AND `snapshot`.`route_version_id` = `task`.`route_version_id`
     AND `snapshot`.`route_process_id` = `regulation`.`route_process_id`
     AND `snapshot`.`process_id` = `regulation`.`process_id`
     AND `snapshot`.`deleted` = b'0'
   WHERE `task`.`deleted` = b'0'
     AND `task`.`task_status` <> 'CANCELLED'
     AND (
       `version`.`id` IS NULL
       OR `regulation`.`id` IS NULL
       OR `snapshot`.`id` IS NULL
     );
  IF v_missing_formal_source_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC task process identity closure requires formal QA regulation version and active order snapshot sources';
  END IF;

  SELECT COUNT(1)
    INTO v_duplicate_identity_count
    FROM (
      SELECT `task`.`tenant_id`,
             `task`.`active_order_id`,
             CASE
               WHEN `task`.`route_process_id` IS NULL THEN `regulation`.`route_process_id`
               ELSE `task`.`route_process_id`
             END AS `target_route_process_id`,
             `task`.`inspection_type`,
             `task`.`business_date`,
             `task`.`shift_code`,
             `task`.`round_no`,
             `task`.`deleted`
        FROM `mes_pqc_inspection_task` `task`
        JOIN `mes_qa_inspection_regulation_version` `version`
          ON `version`.`tenant_id` = `task`.`tenant_id`
         AND `version`.`id` = `task`.`regulation_version_id`
         AND `version`.`deleted` = b'0'
        JOIN `mes_qa_inspection_regulation` `regulation`
          ON `regulation`.`tenant_id` = `task`.`tenant_id`
         AND `regulation`.`id` = `version`.`regulation_id`
         AND `regulation`.`route_id` = `task`.`route_id`
         AND `regulation`.`route_version_id` = `task`.`route_version_id`
         AND `regulation`.`deleted` = b'0'
       WHERE `task`.`deleted` = b'0'
         AND `task`.`task_status` <> 'CANCELLED'
       GROUP BY `task`.`tenant_id`, `task`.`active_order_id`, `target_route_process_id`,
                `task`.`inspection_type`, `task`.`business_date`, `task`.`shift_code`,
                `task`.`round_no`, `task`.`deleted`
      HAVING COUNT(1) > 1
    ) `duplicate_task_identity`;
  IF v_duplicate_identity_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC task process identity closure duplicate task identities after backfill';
  END IF;

  UPDATE `mes_pqc_inspection_task` `task`
    JOIN `mes_qa_inspection_regulation_version` `version`
      ON `version`.`tenant_id` = `task`.`tenant_id`
     AND `version`.`id` = `task`.`regulation_version_id`
     AND `version`.`deleted` = b'0'
    JOIN `mes_qa_inspection_regulation` `regulation`
      ON `regulation`.`tenant_id` = `task`.`tenant_id`
     AND `regulation`.`id` = `version`.`regulation_id`
     AND `regulation`.`route_id` = `task`.`route_id`
     AND `regulation`.`route_version_id` = `task`.`route_version_id`
     AND `regulation`.`deleted` = b'0'
    JOIN `mes_pro_process_pool_active_order_process_snapshot` `snapshot`
      ON `snapshot`.`tenant_id` = `task`.`tenant_id`
     AND `snapshot`.`active_order_id` = `task`.`active_order_id`
     AND `snapshot`.`work_order_id` = `task`.`work_order_id`
     AND `snapshot`.`route_id` = `task`.`route_id`
     AND `snapshot`.`route_version_id` = `task`.`route_version_id`
     AND `snapshot`.`route_process_id` = `regulation`.`route_process_id`
     AND `snapshot`.`process_id` = `regulation`.`process_id`
     AND `snapshot`.`deleted` = b'0'
     SET `task`.`route_process_id` = `regulation`.`route_process_id`,
         `task`.`process_id` = `regulation`.`process_id`
   WHERE `task`.`deleted` = b'0'
     AND `task`.`task_status` <> 'CANCELLED'
     AND (`task`.`route_process_id` IS NULL OR `task`.`process_id` IS NULL);

  SELECT COUNT(1)
    INTO v_unresolved_identity_count
    FROM `mes_pqc_inspection_task`
   WHERE `deleted` = b'0'
     AND `task_status` <> 'CANCELLED'
     AND (`route_process_id` IS NULL OR `process_id` IS NULL);
  IF v_unresolved_identity_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC task process identity closure unresolved null process identities';
  END IF;

  SELECT COUNT(1)
    INTO v_mismatched_identity_count
    FROM `mes_pqc_inspection_task` `task`
    JOIN `mes_qa_inspection_regulation_version` `version`
      ON `version`.`tenant_id` = `task`.`tenant_id`
     AND `version`.`id` = `task`.`regulation_version_id`
     AND `version`.`deleted` = b'0'
    JOIN `mes_qa_inspection_regulation` `regulation`
      ON `regulation`.`tenant_id` = `task`.`tenant_id`
     AND `regulation`.`id` = `version`.`regulation_id`
     AND `regulation`.`deleted` = b'0'
   WHERE `task`.`deleted` = b'0'
     AND `task`.`task_status` <> 'CANCELLED'
     AND (
       `task`.`route_process_id` <> `regulation`.`route_process_id`
       OR `task`.`process_id` <> `regulation`.`process_id`
     );
  IF v_mismatched_identity_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC task process identity closure found task identity mismatched with regulation version';
  END IF;

  ALTER TABLE `mes_pqc_inspection_task`
    MODIFY COLUMN `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    MODIFY COLUMN `process_id` bigint NOT NULL COMMENT '工序ID';
END$$
DELIMITER ;

CALL close_mes_pqc_task_process_identity();

DROP PROCEDURE IF EXISTS close_mes_pqc_task_process_identity;

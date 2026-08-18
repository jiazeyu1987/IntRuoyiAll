-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_pqc_dcc_qa_c00_postflight; type=schema; riskLevel=medium
-- PQC FIRST/PATROL task identity is scoped by the published QA inspection item.

DROP PROCEDURE IF EXISTS migrate_mes_pqc_item_level_task_identity;
DELIMITER $$

CREATE PROCEDURE migrate_mes_pqc_item_level_task_identity()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_duplicate_count INT DEFAULT 0;

  SELECT COUNT(*) INTO v_count
    FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND column_name = 'qa_item_code';
  IF v_count = 0 THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD COLUMN `qa_item_code` varchar(64) NOT NULL DEFAULT ''
        COMMENT 'QA inspection item code; empty only for process-scoped FINAL tasks'
        AFTER `qa_process_id`;
  END IF;

  UPDATE `mes_pqc_inspection_task`
     SET `qa_item_code` = ''
   WHERE `qa_item_code` IS NULL;

  ALTER TABLE `mes_pqc_inspection_task`
    MODIFY COLUMN `qa_item_code` varchar(64) NOT NULL DEFAULT ''
      COMMENT 'QA inspection item code; empty only for process-scoped FINAL tasks';

  SELECT COUNT(*) INTO v_duplicate_count
    FROM (
      SELECT `tenant_id`, `active_order_id`, `regulation_version_id`, `qa_process_id`,
             `qa_item_code`, `inspection_rule_key`, `business_date`, `deleted`
        FROM `mes_pqc_inspection_task`
       WHERE `inspection_rule_key` IS NOT NULL
       GROUP BY `tenant_id`, `active_order_id`, `regulation_version_id`, `qa_process_id`,
                `qa_item_code`, `inspection_rule_key`, `business_date`, `deleted`
      HAVING COUNT(*) > 1
    ) duplicate_identity;
  IF v_duplicate_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'duplicate PQC item-level task identities exist';
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND index_name = 'uk_mes_pqc_task_item_rule_identity';
  IF v_count = 0 THEN
    SELECT COUNT(*) INTO v_count
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND index_name = 'uk_mes_pqc_task_qa_identity';
    IF v_count > 0 THEN
      ALTER TABLE `mes_pqc_inspection_task` DROP INDEX `uk_mes_pqc_task_qa_identity`;
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND index_name = 'uk_mes_pqc_task_rule_identity';
    IF v_count > 0 THEN
      ALTER TABLE `mes_pqc_inspection_task` DROP INDEX `uk_mes_pqc_task_rule_identity`;
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND index_name = 'uk_mes_pqc_task_identity';
    IF v_count > 0 THEN
      ALTER TABLE `mes_pqc_inspection_task` DROP INDEX `uk_mes_pqc_task_identity`;
    END IF;

    ALTER TABLE `mes_pqc_inspection_task`
      ADD UNIQUE KEY `uk_mes_pqc_task_item_rule_identity`
        (`tenant_id`, `active_order_id`, `regulation_version_id`, `qa_process_id`,
         `qa_item_code`, `inspection_rule_key`, `business_date`, `deleted`);
  END IF;
END$$

DELIMITER ;
CALL migrate_mes_pqc_item_level_task_identity();
DROP PROCEDURE IF EXISTS migrate_mes_pqc_item_level_task_identity;

SELECT COUNT(*) = 1 AS qa_item_code_ready
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pqc_inspection_task'
   AND column_name = 'qa_item_code'
   AND is_nullable = 'NO';

SELECT COUNT(*) = 8 AS item_rule_identity_ready
  FROM information_schema.statistics
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pqc_inspection_task'
   AND index_name = 'uk_mes_pqc_task_item_rule_identity';

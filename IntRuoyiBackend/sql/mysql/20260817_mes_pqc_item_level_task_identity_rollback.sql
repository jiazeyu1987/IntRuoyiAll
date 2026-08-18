-- Rollback requires all item-scoped task data to be removed by an approved data rollback first.

DROP PROCEDURE IF EXISTS rollback_mes_pqc_item_level_task_identity;
DELIMITER $$

CREATE PROCEDURE rollback_mes_pqc_item_level_task_identity()
BEGIN
  DECLARE v_count INT DEFAULT 0;

  SELECT COUNT(*) INTO v_count
    FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND column_name = 'qa_item_code';
  IF v_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'qa_item_code is missing; item-level identity rollback is not applicable';
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM `mes_pqc_inspection_task`
   WHERE `qa_item_code` <> '';
  IF v_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'item-scoped PQC tasks exist; run an approved data rollback before schema rollback';
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND index_name = 'uk_mes_pqc_task_item_rule_identity';
  IF v_count > 0 THEN
    ALTER TABLE `mes_pqc_inspection_task`
      DROP INDEX `uk_mes_pqc_task_item_rule_identity`;
  END IF;

  ALTER TABLE `mes_pqc_inspection_task`
    ADD UNIQUE KEY `uk_mes_pqc_task_rule_identity`
      (`tenant_id`, `active_order_id`, `regulation_version_id`, `qa_process_id`,
       `inspection_rule_key`, `business_date`, `deleted`),
    DROP COLUMN `qa_item_code`;
END$$

DELIMITER ;
CALL rollback_mes_pqc_item_level_task_identity();
DROP PROCEDURE IF EXISTS rollback_mes_pqc_item_level_task_identity;

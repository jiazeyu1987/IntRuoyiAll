-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260817_mes_pqc_item_level_task_identity; type=schema; riskLevel=medium
-- PQC task identity must include the frozen production process so every active-order process can own one final inspection block.

DROP PROCEDURE IF EXISTS migrate_mes_pqc_task_process_item_identity;
DELIMITER $$

CREATE PROCEDURE migrate_mes_pqc_task_process_item_identity()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_duplicate_count INT DEFAULT 0;

  SELECT COUNT(*) INTO v_duplicate_count
    FROM (
      SELECT `tenant_id`, `active_order_id`, `route_process_id`, `process_id`,
             `regulation_version_id`, `qa_process_id`, `qa_item_code`,
             `inspection_rule_key`, `business_date`, `deleted`
        FROM `mes_pqc_inspection_task`
       WHERE `inspection_rule_key` IS NOT NULL
       GROUP BY `tenant_id`, `active_order_id`, `route_process_id`, `process_id`,
                `regulation_version_id`, `qa_process_id`, `qa_item_code`,
                `inspection_rule_key`, `business_date`, `deleted`
      HAVING COUNT(*) > 1
    ) duplicate_identity;
  IF v_duplicate_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'duplicate PQC process item-level task identities exist';
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND index_name = 'uk_mes_pqc_task_process_item_rule_identity';
  IF v_count = 0 THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD UNIQUE KEY `uk_mes_pqc_task_process_item_rule_identity`
        (`tenant_id`, `active_order_id`, `route_process_id`, `process_id`,
         `regulation_version_id`, `qa_process_id`, `qa_item_code`,
         `inspection_rule_key`, `business_date`, `deleted`);
  END IF;

  SELECT COUNT(*) INTO v_count
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pqc_inspection_task'
     AND index_name = 'uk_mes_pqc_task_item_rule_identity';
  IF v_count > 0 THEN
    ALTER TABLE `mes_pqc_inspection_task` DROP INDEX `uk_mes_pqc_task_item_rule_identity`;
  END IF;
END$$

DELIMITER ;
CALL migrate_mes_pqc_task_process_item_identity();
DROP PROCEDURE IF EXISTS migrate_mes_pqc_task_process_item_identity;

SELECT COUNT(*) = 10 AS process_item_rule_identity_ready
  FROM information_schema.statistics
 WHERE table_schema = DATABASE()
   AND table_name = 'mes_pqc_inspection_task'
   AND index_name = 'uk_mes_pqc_task_process_item_rule_identity';

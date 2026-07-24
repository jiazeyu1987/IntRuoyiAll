-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_edhr_candidate_pool_column;

DELIMITER //

CREATE PROCEDURE ensure_mes_edhr_candidate_pool_column(
    IN target_table varchar(64),
    IN target_column varchar(64),
    IN alter_sql text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
      AND COLUMN_NAME = target_column
  ) THEN
    SET @ddl = alter_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//

DELIMITER ;

CALL ensure_mes_edhr_candidate_pool_column(
  'mes_pro_edhr_work_task_assignment_rule',
  'candidate_source_type',
  'ALTER TABLE `mes_pro_edhr_work_task_assignment_rule` ADD COLUMN `candidate_source_type` varchar(32) DEFAULT NULL COMMENT ''候选来源类型：USER/USER_GROUP/ROLE_GROUP/DEPT_GROUP'' AFTER `review_user_id`'
);

CALL ensure_mes_edhr_candidate_pool_column(
  'mes_pro_edhr_work_task_assignment_rule',
  'candidate_source_id',
  'ALTER TABLE `mes_pro_edhr_work_task_assignment_rule` ADD COLUMN `candidate_source_id` bigint DEFAULT NULL COMMENT ''候选来源ID'' AFTER `candidate_source_type`'
);

CALL ensure_mes_edhr_candidate_pool_column(
  'mes_pro_edhr_work_task',
  'candidate_source_type',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `candidate_source_type` varchar(32) DEFAULT NULL COMMENT ''候选来源类型快照'' AFTER `assignee_user_id`'
);

CALL ensure_mes_edhr_candidate_pool_column(
  'mes_pro_edhr_work_task',
  'candidate_source_id',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `candidate_source_id` bigint DEFAULT NULL COMMENT ''候选来源ID快照'' AFTER `candidate_source_type`'
);

CALL ensure_mes_edhr_candidate_pool_column(
  'mes_pro_edhr_work_task',
  'candidate_user_snapshot',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `candidate_user_snapshot` varchar(1000) DEFAULT NULL COMMENT ''候选用户ID快照，逗号分隔'' AFTER `candidate_source_id`'
);

UPDATE `mes_pro_edhr_work_task_assignment_rule`
SET `candidate_source_type` = 'USER',
    `candidate_source_id` = `assignee_user_id`
WHERE (`candidate_source_type` IS NULL OR `candidate_source_type` = '')
  AND `assignee_user_id` IS NOT NULL;

UPDATE `mes_pro_edhr_work_task`
SET `candidate_source_type` = 'USER',
    `candidate_source_id` = `assignee_user_id`,
    `candidate_user_snapshot` = CAST(`assignee_user_id` AS CHAR)
WHERE (`candidate_source_type` IS NULL OR `candidate_source_type` = '')
  AND `assignee_user_id` IS NOT NULL;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_candidate_pool_column;

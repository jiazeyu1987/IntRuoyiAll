-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260614_mes_edhr_work_task_candidate_pool; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_edhr_assignment_rule_candidate_nullable;

DELIMITER //

CREATE PROCEDURE ensure_mes_edhr_assignment_rule_candidate_nullable()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_work_task_assignment_rule'
      AND COLUMN_NAME = 'assignee_user_id'
      AND IS_NULLABLE = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task_assignment_rule`
      MODIFY COLUMN `assignee_user_id` bigint DEFAULT NULL COMMENT '任务责任人用户ID；候选池规则可为空';
  END IF;
END//

DELIMITER ;

CALL ensure_mes_edhr_assignment_rule_candidate_nullable();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_assignment_rule_candidate_nullable;

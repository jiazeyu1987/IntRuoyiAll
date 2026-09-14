-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260804_mes_process_pool_timeline_performance_indexes; type=schema; riskLevel=medium
-- AC-M16: enforce one terminal review fact per process-pool submission event.

DROP PROCEDURE IF EXISTS ensure_mes_pp_ac_m16_terminal_constraints;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_ac_m16_terminal_constraints()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM (
      SELECT `tenant_id`, `event_id`, `deleted`, COUNT(*) AS `review_count`
      FROM `mes_pro_process_pool_submission_review`
      GROUP BY `tenant_id`, `event_id`, `deleted`
      HAVING COUNT(*) > 1
    ) AS `duplicate_reviews`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate MES process-pool submission reviews block AC-M16 terminal constraint';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_submission_review'
      AND index_name = 'uk_mes_pp_submission_review_event'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_submission_review`
      ADD UNIQUE KEY `uk_mes_pp_submission_review_event` (`tenant_id`, `event_id`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_ac_m16_terminal_constraints();

DROP PROCEDURE IF EXISTS ensure_mes_pp_ac_m16_terminal_constraints;

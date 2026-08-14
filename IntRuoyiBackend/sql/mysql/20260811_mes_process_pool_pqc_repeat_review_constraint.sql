-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_process_pool_ac_m20_pqc_review_closure; type=schema; riskLevel=medium
-- Allow PQC review history while preserving one active terminal review per production event.

DROP PROCEDURE IF EXISTS migrate_mes_pp_pqc_repeat_review_constraint;
DELIMITER $$
CREATE PROCEDURE migrate_mes_pp_pqc_repeat_review_constraint()
BEGIN
  DECLARE production_duplicate_count bigint DEFAULT 0;
  DECLARE invalid_leader_type_count bigint DEFAULT 0;
  DECLARE replacement_index_columns varchar(255) DEFAULT NULL;
  DECLARE replacement_index_non_unique int DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_submission_review table';
  END IF;

  IF (
    SELECT COUNT(1)
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND column_name IN ('tenant_id', 'event_id', 'leader_type', 'deleted')
  ) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Review constraint migration requires tenant_id, event_id, leader_type and deleted';
  END IF;

  SELECT COUNT(1)
    INTO invalid_leader_type_count
    FROM `mes_pro_process_pool_submission_review`
   WHERE `leader_type` NOT IN ('PRODUCTION', 'PQC');

  IF invalid_leader_type_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unsupported leader_type blocks review constraint migration';
  END IF;

  SELECT COUNT(1)
    INTO production_duplicate_count
    FROM (
      SELECT `tenant_id`, `event_id`
        FROM `mes_pro_process_pool_submission_review`
       WHERE `leader_type` = 'PRODUCTION'
         AND `deleted` = b'0'
       GROUP BY `tenant_id`, `event_id`
      HAVING COUNT(1) > 1
    ) duplicate_production_reviews;

  IF production_duplicate_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate active production reviews block review constraint migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND column_name = 'production_terminal_event_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_submission_review`
      ADD COLUMN `production_terminal_event_id` bigint GENERATED ALWAYS AS (CASE WHEN `leader_type` = 'PRODUCTION' AND `deleted` = b'0' THEN `event_id` ELSE NULL END) STORED
        COMMENT 'Active production review event identity; PQC history remains repeatable'
        AFTER `leader_type`;
  ELSEIF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND column_name = 'production_terminal_event_id'
       AND data_type = 'bigint'
       AND extra LIKE '%STORED GENERATED%'
       AND generation_expression LIKE '%leader_type%'
       AND generation_expression LIKE '%PRODUCTION%'
       AND generation_expression LIKE '%deleted%'
       AND generation_expression LIKE '%event_id%'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing production_terminal_event_id has an incompatible definition';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND index_name = 'uk_mes_pp_submission_review_production_terminal'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_submission_review`
      ADD UNIQUE KEY `uk_mes_pp_submission_review_production_terminal` (`tenant_id`, `production_terminal_event_id`);
  ELSE
    SELECT GROUP_CONCAT(`column_name` ORDER BY `seq_in_index` SEPARATOR ','), MIN(`non_unique`)
      INTO replacement_index_columns, replacement_index_non_unique
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND index_name = 'uk_mes_pp_submission_review_production_terminal';

    IF replacement_index_columns <> 'tenant_id,production_terminal_event_id'
        OR replacement_index_non_unique <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Existing production review terminal index has an incompatible definition';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND index_name = 'uk_mes_pp_submission_review_event_terminal'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_submission_review`
      DROP INDEX `uk_mes_pp_submission_review_event_terminal`;
  END IF;

  IF EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND index_name = 'uk_mes_pp_submission_review_event'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_submission_review`
      DROP INDEX `uk_mes_pp_submission_review_event`;
  END IF;
END$$
DELIMITER ;

CALL migrate_mes_pp_pqc_repeat_review_constraint();

DROP PROCEDURE IF EXISTS migrate_mes_pp_pqc_repeat_review_constraint;

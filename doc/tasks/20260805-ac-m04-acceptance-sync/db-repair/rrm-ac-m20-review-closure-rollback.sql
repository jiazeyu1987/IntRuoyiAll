-- Local-only guarded rollback for the AC-M20 review-closure migration.
-- This rollback intentionally fails after new review or aggregation facts are written.

DROP PROCEDURE IF EXISTS rollback_rrm_ac_m20_review_closure;
DELIMITER $$
CREATE PROCEDURE rollback_rrm_ac_m20_review_closure()
BEGIN
  DECLARE v_review_rows bigint DEFAULT 0;
  DECLARE v_invalid_leader_type_rows bigint DEFAULT 0;
  DECLARE v_aggregate_rows bigint DEFAULT 0;
  DECLARE v_column_exists int DEFAULT 0;
  DECLARE v_index_exists int DEFAULT 0;
  DECLARE v_table_exists int DEFAULT 0;

  IF DATABASE() <> 'ruoyi-vue-pro' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'AC-M20 rollback is restricted to local ruoyi-vue-pro';
  END IF;

  SELECT COUNT(*)
    INTO v_review_rows
    FROM mes_pro_process_pool_submission_review;
  IF v_review_rows <> 75 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'AC-M20 rollback blocked because review rows changed after migration';
  END IF;

  SELECT COUNT(*)
    INTO v_column_exists
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'mes_pro_process_pool_submission_review'
     AND COLUMN_NAME = 'leader_type';

  IF v_column_exists = 1 THEN
    SELECT COUNT(*)
      INTO v_invalid_leader_type_rows
      FROM mes_pro_process_pool_submission_review review_row
      JOIN mes_pro_process_pool_event event_row
        ON event_row.tenant_id = review_row.tenant_id
       AND event_row.id = review_row.event_id
       AND event_row.deleted = b'0'
     WHERE review_row.deleted = b'0'
       AND review_row.leader_type <> CASE
         WHEN event_row.event_type = 'PQC_INSPECTION' THEN 'PQC'
         ELSE 'PRODUCTION'
       END;
    IF v_invalid_leader_type_rows <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'AC-M20 rollback blocked by unexpected leader_type values';
    END IF;
  END IF;

  SELECT COUNT(*)
    INTO v_table_exists
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'mes_pqc_process_inspection_aggregate_detail';
  IF v_table_exists = 1 THEN
    SELECT COUNT(*)
      INTO v_aggregate_rows
      FROM mes_pqc_process_inspection_aggregate_detail;
    IF v_aggregate_rows <> 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'AC-M20 rollback blocked because aggregate facts already exist';
    END IF;
    DROP TABLE mes_pqc_process_inspection_aggregate_detail;
  END IF;

  SELECT COUNT(*)
    INTO v_index_exists
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'mes_pro_process_pool_submission_review'
     AND INDEX_NAME = 'uk_mes_pp_submission_review_event_terminal';
  IF v_index_exists > 0 THEN
    ALTER TABLE mes_pro_process_pool_submission_review
      DROP INDEX uk_mes_pp_submission_review_event_terminal;
  END IF;

  IF v_column_exists = 1 THEN
    ALTER TABLE mes_pro_process_pool_submission_review
      DROP COLUMN leader_type;
  END IF;

  SET @old_task_status_comment =
    CONVERT(UNHEX('C3A4C2BBC2BBC3A5C5A0C2A1C3A7C5A0C2B6C3A6E282ACC281C3AFC2BCC5A150454E44494E472F5355424D49545445442F43414E43454C4C4544') USING utf8mb4);
  SET @restore_task_status_sql = CONCAT(
    'ALTER TABLE mes_pqc_inspection_task MODIFY COLUMN task_status ',
    'varchar(32) NOT NULL COMMENT ',
    QUOTE(@old_task_status_comment)
  );
  PREPARE restore_task_status_stmt FROM @restore_task_status_sql;
  EXECUTE restore_task_status_stmt;
  DEALLOCATE PREPARE restore_task_status_stmt;
END$$
DELIMITER ;

CALL rollback_rrm_ac_m20_review_closure();
DROP PROCEDURE IF EXISTS rollback_rrm_ac_m20_review_closure;

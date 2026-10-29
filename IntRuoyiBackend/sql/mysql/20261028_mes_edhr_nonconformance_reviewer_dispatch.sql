-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261005_mes_active_order_handoff,20260920_mes_edhr_nonconformance_review_active_order_fact; type=schema; riskLevel=medium
-- QA responsibility is explicitly chosen by the creator. Never infer PQC leaders or backfill an arbitrary account.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_edhr_ncr_reviewer_dispatch;
DELIMITER $$
CREATE PROCEDURE migrate_edhr_ncr_reviewer_dispatch()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_pro_edhr_nonconformance_review' AND column_name='qa_reviewer_user_id') THEN
    ALTER TABLE mes_pro_edhr_nonconformance_review ADD COLUMN qa_reviewer_user_id BIGINT NULL
      COMMENT 'Explicitly selected QA reviewer; historical pending rows require manual assignment';
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_pro_edhr_nonconformance_review' AND column_name='qa_reviewer_user_id'
      AND data_type='bigint' AND is_nullable='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible NCR QA reviewer column';
  END IF;
END$$
DELIMITER ;
CALL migrate_edhr_ncr_reviewer_dispatch();
DROP PROCEDURE migrate_edhr_ncr_reviewer_dispatch;

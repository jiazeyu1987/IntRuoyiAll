-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261001_dcc_b_reserved_draft_round; type=schema; riskLevel=medium
-- Only new obsolete form drafts record their real form identity. Historical/native rows remain NULL.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_obsolete_form_draft_round;
DELIMITER //
CREATE PROCEDURE ensure_dcc_obsolete_form_draft_round()
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_application_round_link' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 requires the formal InnoDB application round mapping';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='form_instance_id') THEN
    ALTER TABLE dcc_application_round_link ADD COLUMN form_instance_id BIGINT NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='form_instance_id'
      AND DATA_TYPE='bigint' AND COLUMN_TYPE NOT LIKE '%unsigned%' AND IS_NULLABLE='YES'
      AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 form identity column has schema drift';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_application_round_link' AND INDEX_NAME='uk_dcc_round_form') THEN
    ALTER TABLE dcc_application_round_link ADD UNIQUE KEY uk_dcc_round_form(tenant_id,form_instance_id);
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_application_round_link' AND INDEX_NAME='uk_dcc_round_form')<>2
      OR NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_application_round_link' AND INDEX_NAME='uk_dcc_round_form'
        AND NON_UNIQUE=0 AND SEQ_IN_INDEX=1 AND COLUMN_NAME='tenant_id' AND SUB_PART IS NULL)
      OR NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_application_round_link' AND INDEX_NAME='uk_dcc_round_form'
        AND NON_UNIQUE=0 AND SEQ_IN_INDEX=2 AND COLUMN_NAME='form_instance_id' AND SUB_PART IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 form identity index has schema drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_obsolete_form_draft_round();
DROP PROCEDURE IF EXISTS ensure_dcc_obsolete_form_draft_round;

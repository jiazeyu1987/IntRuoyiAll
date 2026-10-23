-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260920_dcc_controlled_file_attachment,20260911_dcc_upload_ticket_category_guard; type=schema; riskLevel=medium
-- IC03 W06: nullable training attachment facts only. Historical ordinary attachments remain unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_training_attachment_facts;
DROP PROCEDURE IF EXISTS check_dcc_training_attach_column;
DELIMITER //
CREATE PROCEDURE check_dcc_training_attach_column(IN column_id VARCHAR(64),IN expected_type VARCHAR(64))
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME=column_id
      AND COLUMN_TYPE=expected_type AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC03 nullable attachment column definition drift';
  END IF;
END//
CREATE PROCEDURE ensure_dcc_training_attachment_facts()
BEGIN
  DECLARE key_expression LONGTEXT;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC03 requires the actual InnoDB attachment table creation migration';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='attachment_purpose') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD COLUMN attachment_purpose VARCHAR(32) NULL COMMENT '附件用途：ATTACHMENT/TRAINING_RECORD';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='application_process_instance_id') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD COLUMN application_process_instance_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '本次申请流程轮次';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='uploaded_by') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD COLUMN uploaded_by BIGINT NULL COMMENT '后台绑定完成时上传人';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='uploaded_username_snapshot') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD COLUMN uploaded_username_snapshot VARCHAR(128) NULL COMMENT '上传人用户名快照';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='uploaded_nickname_snapshot') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD COLUMN uploaded_nickname_snapshot VARCHAR(128) NULL COMMENT '上传人昵称快照';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='uploaded_at') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD COLUMN uploaded_at DATETIME NULL COMMENT '后台绑定完成时间';
  END IF;
  CALL check_dcc_training_attach_column('attachment_purpose','varchar(32)');
  CALL check_dcc_training_attach_column('application_process_instance_id','varchar(128)');
  CALL check_dcc_training_attach_column('uploaded_by','bigint');
  CALL check_dcc_training_attach_column('uploaded_username_snapshot','varchar(128)');
  CALL check_dcc_training_attach_column('uploaded_nickname_snapshot','varchar(128)');
  CALL check_dcc_training_attach_column('uploaded_at','datetime');
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='training_record_round_key') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD COLUMN training_record_round_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin
      GENERATED ALWAYS AS (CASE WHEN attachment_purpose='TRAINING_RECORD' AND deleted=0
        THEN application_process_instance_id ELSE NULL END) STORED;
  ELSE
    SELECT GENERATION_EXPRESSION INTO key_expression FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_attachment'
        AND COLUMN_NAME='training_record_round_key';
    -- MySQL may render parentheses/backticks/whitespace/charset introducers and bit zero differently.
    -- Normalize syntax only, then compare the full expression; token-presence is insufficient.
    SET key_expression=REPLACE(REPLACE(REPLACE(LOWER(key_expression),'_utf8mb4',''), '_utf8mb3',''),'b''0''','0');
    SET key_expression=REPLACE(key_expression,CONCAT(CHAR(92),CHAR(39)),CHAR(39));
    SET key_expression=REGEXP_REPLACE(key_expression,'[`()[:space:]]','');
    IF key_expression IS NULL OR key_expression<>'casewhenattachment_purpose=''training_record''anddeleted=0thenapplication_process_instance_idelsenullend' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='training_record_round_key generated expression is incompatible with IC03';
    END IF;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='training_record_round_key'
      AND COLUMN_TYPE='varchar(128)' AND IS_NULLABLE='YES' AND EXTRA='STORED GENERATED'
      AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='training_record_round_key generated column type drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND COLUMN_NAME='application_process_instance_id'
      AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC03 exact process instance collation drift';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND INDEX_NAME='uk_dcc_training_attachment_round') THEN
    ALTER TABLE dcc_controlled_file_attachment ADD UNIQUE KEY uk_dcc_training_attachment_round
      (tenant_id,controlled_file_id,training_record_round_key);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_attachment' AND INDEX_NAME='uk_dcc_training_attachment_round'
      GROUP BY INDEX_NAME HAVING COUNT(*)=3 AND MIN(NON_UNIQUE)=0 AND MAX(NON_UNIQUE)=0
        AND SUM(SUB_PART IS NOT NULL)=0 AND MIN(INDEX_TYPE)='BTREE' AND MAX(INDEX_TYPE)='BTREE'
        AND GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')='tenant_id,controlled_file_id,training_record_round_key') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC03 training attachment uniqueness definition drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_training_attachment_facts();
DROP PROCEDURE IF EXISTS ensure_dcc_training_attachment_facts;
DROP PROCEDURE IF EXISTS check_dcc_training_attach_column;

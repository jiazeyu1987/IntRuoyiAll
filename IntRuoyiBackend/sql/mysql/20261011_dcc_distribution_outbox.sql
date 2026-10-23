-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema,20260930_dcc_a_lifecycle,20261011_dcc_application_effective_distribution; type=schema; riskLevel=medium
-- IC10 additive distribution outbox. No historical DML/backfill and no runtime audit-policy seed.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_distribution_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  source_event_id BIGINT NOT NULL,
  event_key VARCHAR(255) NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  master_id BIGINT NOT NULL,
  controlled_file_id BIGINT NOT NULL,
  approval_process_instance_id VARCHAR(128) NOT NULL,
  version_no VARCHAR(64) NOT NULL,
  source_occurred_at DATETIME(6) NOT NULL,
  department_ids_json TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  snapshot_at DATETIME(6) NULL,
  snapshot_policy_version VARCHAR(128) NULL,
  snapshot_payload_hash CHAR(64) NULL,
  event_payload_hash CHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  error_code VARCHAR(64) NULL,
  error_message TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  failure_at DATETIME(6) NULL,
  dispatch_prepared_at DATETIME(6) NULL,
  sent_at DATETIME(6) NULL,
  row_version BIGINT NOT NULL DEFAULT 0,
  creator VARCHAR(64) NULL,
  updater VARCHAR(64) NULL,
  create_time DATETIME(6) NOT NULL,
  update_time DATETIME(6) NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dcc_dist_batch_event (tenant_id,source_event_id),
  UNIQUE KEY uk_dcc_dist_batch_key (tenant_id,event_key),
  UNIQUE KEY uk_dcc_dist_batch_version (tenant_id,controlled_file_id,approval_process_instance_id),
  KEY idx_dcc_dist_batch_recovery (tenant_id,status,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dcc_distribution_dispatch_attempt (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  message_job_id BIGINT NOT NULL,
  attempt_no INT NOT NULL,
  business_key VARCHAR(255) NOT NULL,
  recipient_user_id BIGINT NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  platform_message_id BIGINT NULL,
  started_at DATETIME(6) NOT NULL,
  finished_at DATETIME(6) NULL,
  error_code VARCHAR(64) NULL,
  error_message TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  creator VARCHAR(64) NULL,
  updater VARCHAR(64) NULL,
  create_time DATETIME(6) NOT NULL,
  update_time DATETIME(6) NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dcc_dist_attempt_job_no (tenant_id,message_job_id,attempt_no),
  KEY idx_dcc_dist_attempt_batch (tenant_id,batch_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS dcc_ic10_check_column;
DROP PROCEDURE IF EXISTS dcc_ic10_check_index;
DROP PROCEDURE IF EXISTS dcc_ic10_apply;
DELIMITER $$
CREATE PROCEDURE dcc_ic10_check_column(IN t VARCHAR(64),IN c VARCHAR(64),IN kind VARCHAR(64),IN nullable_flag VARCHAR(3),IN charset_value VARCHAR(64),IN collation_value VARCHAR(64),IN extra_value VARCHAR(64))
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=t AND COLUMN_NAME=c
    AND LOWER(COLUMN_TYPE)=kind AND IS_NULLABLE=nullable_flag
    AND (charset_value IS NULL OR CHARACTER_SET_NAME=charset_value) AND (collation_value IS NULL OR COLLATION_NAME=collation_value)
    AND (extra_value IS NULL OR EXTRA=extra_value)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC10 existing column definition mismatch';
  END IF;
END$$
CREATE PROCEDURE dcc_ic10_check_index(IN t VARCHAR(64),IN idx VARCHAR(64),IN expected_columns VARCHAR(512),IN expected_non_unique INT)
BEGIN
  DECLARE actual_columns VARCHAR(512);
  DECLARE invalid_count INT;
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),
         SUM(NON_UNIQUE<>expected_non_unique OR SUB_PART IS NOT NULL OR INDEX_TYPE<>'BTREE')
    INTO actual_columns,invalid_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=t AND INDEX_NAME=idx;
  IF actual_columns IS NULL OR actual_columns<>expected_columns OR invalid_count<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC10 existing index definition mismatch';
  END IF;
END$$
CREATE PROCEDURE dcc_ic10_apply()
BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('dcc_distribution_batch','dcc_distribution_dispatch_attempt')
      AND (ENGINE<>'InnoDB' OR TABLE_COLLATION<>'utf8mb4_unicode_ci')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC10 existing table engine or collation mismatch';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_message_job' AND COLUMN_NAME='error_message') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC10 message_job error_message is missing';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_message_job'
      AND COLUMN_NAME='error_message' AND (LOWER(COLUMN_TYPE) NOT IN ('varchar(500)','text') OR IS_NULLABLE<>'YES')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC10 message_job source error column mismatch';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_message_job' AND COLUMN_NAME='error_message') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC10 error_message unexpected indexed column';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_message_job' AND COLUMN_NAME='error_message' AND LOWER(COLUMN_TYPE)='varchar(500)') THEN
    ALTER TABLE dcc_controlled_file_message_job MODIFY COLUMN error_message TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '完整脱敏消息投递失败原因';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_message_job' AND COLUMN_NAME='distribution_batch_id') THEN
    ALTER TABLE dcc_controlled_file_message_job ADD COLUMN distribution_batch_id BIGINT NULL COMMENT '仅新下发任务批次';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_message_job' AND INDEX_NAME='idx_dcc_message_job_distribution_batch') THEN
    ALTER TABLE dcc_controlled_file_message_job ADD KEY idx_dcc_message_job_distribution_batch (tenant_id,distribution_batch_id,id);
  END IF;
  CALL dcc_ic10_check_column('dcc_controlled_file_message_job','error_message','text','YES','utf8mb4','utf8mb4_unicode_ci',NULL);
  CALL dcc_ic10_check_column('dcc_controlled_file_message_job','distribution_batch_id','bigint','YES',NULL,NULL,'');
  CALL dcc_ic10_check_index('dcc_controlled_file_message_job','idx_dcc_message_job_distribution_batch','tenant_id,distribution_batch_id,id',1);
  CALL dcc_ic10_check_column('dcc_distribution_batch','id','bigint','NO',NULL,NULL,'auto_increment');
  CALL dcc_ic10_check_column('dcc_distribution_batch','tenant_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','source_event_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','event_key','varchar(255)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','event_type','varchar(32)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','master_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','controlled_file_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','approval_process_instance_id','varchar(128)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','version_no','varchar(64)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','source_occurred_at','datetime(6)','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','department_ids_json','text','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','snapshot_at','datetime(6)','YES',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','snapshot_policy_version','varchar(128)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','snapshot_payload_hash','char(64)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','event_payload_hash','char(64)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','status','varchar(32)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','error_code','varchar(64)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','error_message','text','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','failure_at','datetime(6)','YES',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','dispatch_prepared_at','datetime(6)','YES',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','sent_at','datetime(6)','YES',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','row_version','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','creator','varchar(64)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','updater','varchar(64)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_batch','create_time','datetime(6)','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','update_time','datetime(6)','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_batch','deleted','tinyint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_index('dcc_distribution_batch','PRIMARY','id',0);
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','id','bigint','NO',NULL,NULL,'auto_increment');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','tenant_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','batch_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','message_job_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','attempt_no','int','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','business_key','varchar(255)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','recipient_user_id','bigint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','template_code','varchar(64)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','status','varchar(32)','NO','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','platform_message_id','bigint','YES',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','started_at','datetime(6)','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','finished_at','datetime(6)','YES',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','error_code','varchar(64)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','error_message','text','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','creator','varchar(64)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','updater','varchar(64)','YES','utf8mb4','utf8mb4_unicode_ci','');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','create_time','datetime(6)','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','update_time','datetime(6)','NO',NULL,NULL,'');
  CALL dcc_ic10_check_column('dcc_distribution_dispatch_attempt','deleted','tinyint','NO',NULL,NULL,'');
  CALL dcc_ic10_check_index('dcc_distribution_dispatch_attempt','PRIMARY','id',0);
  CALL dcc_ic10_check_index('dcc_distribution_batch','uk_dcc_dist_batch_event','tenant_id,source_event_id',0);
  CALL dcc_ic10_check_index('dcc_distribution_batch','uk_dcc_dist_batch_key','tenant_id,event_key',0);
  CALL dcc_ic10_check_index('dcc_distribution_batch','uk_dcc_dist_batch_version','tenant_id,controlled_file_id,approval_process_instance_id',0);
  CALL dcc_ic10_check_index('dcc_distribution_batch','idx_dcc_dist_batch_recovery','tenant_id,status,id',1);
  CALL dcc_ic10_check_index('dcc_distribution_dispatch_attempt','uk_dcc_dist_attempt_job_no','tenant_id,message_job_id,attempt_no',0);
  CALL dcc_ic10_check_index('dcc_distribution_dispatch_attempt','idx_dcc_dist_attempt_batch','tenant_id,batch_id,id',1);
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_distribution_batch'
      AND COLUMN_NAME IN ('row_version','deleted') AND COLUMN_DEFAULT='0')<>2 OR
     (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_distribution_dispatch_attempt'
      AND COLUMN_NAME='deleted' AND COLUMN_DEFAULT='0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC10 existing default definition mismatch';
  END IF;
END$$
DELIMITER ;
CALL dcc_ic10_apply();
DROP PROCEDURE dcc_ic10_apply;
DROP PROCEDURE dcc_ic10_check_column;
DROP PROCEDURE dcc_ic10_check_index;

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_controlled_file_related_file,20260930_dcc_a_signature_workflow_round,20260930_dcc_d_relations; type=schema; riskLevel=medium
-- IC07: new append-only signed node evidence. No historical updates, backfills or free HTTP writes.
-- This file is the current task's new, unexecuted migration source. The additive IC14 base-column
-- validator adjustment is reviewed before release; never overwrite an already deployed migration SHA.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_relation_stage_snapshot (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 source_file_id BIGINT NOT NULL,
 source_master_id BIGINT NOT NULL,
 project_id BIGINT NOT NULL,
 process_instance_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 task_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 obligation_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 stage_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 node_instance_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 department_id BIGINT NULL,
 actor_id BIGINT NOT NULL,
 signature_id BIGINT NOT NULL,
 signed_at DATETIME NOT NULL,
 command_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 payload_hash CHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 expected_digest CHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 complete_digest CHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 action_type VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 reason VARCHAR(2048) NOT NULL,
 UNIQUE KEY uk_dcc_relation_stage_node (tenant_id,process_instance_id,task_id,obligation_id),
 UNIQUE KEY uk_dcc_relation_stage_command (tenant_id,source_file_id,command_key),
 UNIQUE KEY uk_dcc_relation_stage_signature (tenant_id,signature_id),
 KEY idx_dcc_relation_stage_round (tenant_id,source_file_id,process_instance_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS dcc_relation_stage_member (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 snapshot_id BIGINT NOT NULL,
 related_master_id BIGINT NOT NULL,
 selected_file_id BIGINT NOT NULL,
 project_id BIGINT NOT NULL,
 file_number_snapshot VARCHAR(128) NOT NULL,
 file_name_snapshot VARCHAR(256) NOT NULL,
 version_no_snapshot VARCHAR(64) NOT NULL,
 origin VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 supplemental TINYINT NOT NULL,
 assignee_user_id BIGINT NULL,
 due_at DATETIME NULL,
 UNIQUE KEY uk_dcc_relation_stage_member (tenant_id,snapshot_id,related_master_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
DROP PROCEDURE IF EXISTS check_dcc_relation_stage_column;
DROP PROCEDURE IF EXISTS ensure_dcc_relation_stage_schema;
DELIMITER //
CREATE PROCEDURE check_dcc_relation_stage_column(IN table_id VARCHAR(64),IN column_id VARCHAR(64),IN expected_type VARCHAR(64),IN nullable_flag VARCHAR(3),IN extra_flag VARCHAR(32))
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_id
      AND COLUMN_NAME=column_id AND LOWER(COLUMN_TYPE)=expected_type AND IS_NULLABLE=nullable_flag
      AND COLUMN_DEFAULT IS NULL AND LOWER(EXTRA)=extra_flag AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage column definition drift';
  END IF;
END//
CREATE PROCEDURE ensure_dcc_relation_stage_schema()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('dcc_relation_stage_snapshot','dcc_relation_stage_member') AND ENGINE='InnoDB')<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage tables require InnoDB';
  END IF;
  -- Count only the immutable IC07 base contract; separately reviewed IC14 columns remain additive.
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME IN ('id','tenant_id','source_file_id','source_master_id','project_id','process_instance_id','task_id','obligation_id','stage_code','node_instance_id','department_id','actor_id','signature_id','signed_at','command_key','payload_hash','expected_digest','complete_digest','action_type','reason'))<>20 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 unexpected relation stage columns';
  END IF;
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='PRIMARY' AND NON_UNIQUE=0)<>'id' OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='PRIMARY')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage primary key drift';
  END IF;
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','id','bigint','NO','auto_increment');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','tenant_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','source_file_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','source_master_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','project_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','process_instance_id','varchar(128)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','task_id','varchar(128)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','obligation_id','varchar(128)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','stage_code','varchar(64)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','node_instance_id','varchar(128)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','department_id','bigint','YES','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','actor_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','signature_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','signed_at','datetime','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','command_key','varchar(128)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','payload_hash','char(64)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','expected_digest','char(64)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','complete_digest','char(64)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','action_type','varchar(32)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_snapshot','reason','varchar(2048)','NO','');
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='uk_dcc_relation_stage_node' AND NON_UNIQUE=0 AND SUB_PART IS NULL)<>'tenant_id,process_instance_id,task_id,obligation_id' OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='uk_dcc_relation_stage_node')<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage unique index definition drift';
  END IF;
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='uk_dcc_relation_stage_command' AND NON_UNIQUE=0 AND SUB_PART IS NULL)<>'tenant_id,source_file_id,command_key' OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='uk_dcc_relation_stage_command')<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage unique index definition drift';
  END IF;
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='uk_dcc_relation_stage_signature' AND NON_UNIQUE=0 AND SUB_PART IS NULL)<>'tenant_id,signature_id' OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND INDEX_NAME='uk_dcc_relation_stage_signature')<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage unique index definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME IN ('process_instance_id','task_id','obligation_id','stage_code','node_instance_id','command_key','payload_hash','expected_digest','complete_digest','action_type') AND COLLATION_NAME='utf8mb4_bin')<>10 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage identity collation drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_member')<>13 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 unexpected relation stage columns';
  END IF;
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_member' AND INDEX_NAME='PRIMARY' AND NON_UNIQUE=0)<>'id' OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_member' AND INDEX_NAME='PRIMARY')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage member primary key drift';
  END IF;
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','id','bigint','NO','auto_increment');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','tenant_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','snapshot_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','related_master_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','selected_file_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','project_id','bigint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','file_number_snapshot','varchar(128)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','file_name_snapshot','varchar(256)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','version_no_snapshot','varchar(64)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','origin','varchar(32)','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','supplemental','tinyint','NO','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','assignee_user_id','bigint','YES','');
  CALL check_dcc_relation_stage_column('dcc_relation_stage_member','due_at','datetime','YES','');
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_member' AND INDEX_NAME='uk_dcc_relation_stage_member' AND NON_UNIQUE=0 AND SUB_PART IS NULL)<>'tenant_id,snapshot_id,related_master_id' OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_member' AND INDEX_NAME='uk_dcc_relation_stage_member')<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage unique index definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_member' AND COLUMN_NAME IN ('origin') AND COLLATION_NAME='utf8mb4_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC07 relation stage identity collation drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_relation_stage_schema();
DROP PROCEDURE ensure_dcc_relation_stage_schema;
DROP PROCEDURE check_dcc_relation_stage_column;

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261023_system_workbench_projection_check_identity; supersedes=20261011_system_workbench_projection_retention; type=schema; riskLevel=high; applyOrder=10; requiresTargetPreflight=true
-- Candidate only: approved schema backup/maintenance and explicit operator activation are separate.
-- No cleanup, scheduling, engine change or default retention is performed here.
DELIMITER $$
CREATE PROCEDURE wb_s4_retention_check_20261023()
SQL SECURITY INVOKER
BEGIN
 IF DATABASE() IS NULL OR COALESCE(@profile_workbench_s4_maintenance,0)!=1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 retention approved maintenance prerequisite missing';
 END IF;
 IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
     AND TABLE_NAME IN ('system_workbench_global_control','system_workbench_tenant_control','system_workbench_outbox','system_workbench_generation_global','system_workbench_generation','system_workbench_task_projection','system_workbench_task_audience','system_workbench_authorization_projection','system_workbench_consumed_event')
     AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=9 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 retention projection prerequisites missing';
 END IF;
 CREATE TABLE IF NOT EXISTS system_workbench_generation_retirement (
  generation varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, tenant_id bigint NOT NULL,
  state varchar(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  retired_at_millis bigint NOT NULL, purge_not_before_millis bigint NOT NULL,
  retention_seconds bigint NOT NULL, rollback_protection_seconds bigint NOT NULL,
  reason varchar(500) NOT NULL, approval_reference varchar(128) NOT NULL,
  PRIMARY KEY(generation,tenant_id),
  CONSTRAINT ck_wb_retirement_scope CHECK(tenant_id>=0),
  CONSTRAINT ck_wb_retirement_state CHECK(state IN (_utf8mb4'RETIRED',_utf8mb4'PURGED')),
  CONSTRAINT ck_wb_retirement_window CHECK(retired_at_millis>=0 AND purge_not_before_millis>retired_at_millis),
  CONSTRAINT ck_wb_retirement_duration CHECK(retention_seconds>0 AND rollback_protection_seconds>0)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
 IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=1
 OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement')!=9
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='generation' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=1 AND COLLATION_NAME='ascii_bin' AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='tenant_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=2 AND COLLATION_NAME IS NULL AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='state' AND COLUMN_TYPE='varchar(8)' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=3 AND COLLATION_NAME='ascii_bin' AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='retired_at_millis' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=4 AND COLLATION_NAME IS NULL AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='purge_not_before_millis' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=5 AND COLLATION_NAME IS NULL AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='retention_seconds' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=6 AND COLLATION_NAME IS NULL AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='rollback_protection_seconds' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=7 AND COLLATION_NAME IS NULL AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='reason' AND COLUMN_TYPE='varchar(500)' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=8 AND COLLATION_NAME='utf8mb4_0900_bin' AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND COLUMN_NAME='approval_reference' AND COLUMN_TYPE='varchar(128)' AND IS_NULLABLE='NO' AND ORDINAL_POSITION=9 AND COLLATION_NAME='utf8mb4_0900_bin' AND COLUMN_DEFAULT IS NULL AND EXTRA='')
 OR (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement')!=1
 OR NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND INDEX_NAME='PRIMARY' GROUP BY INDEX_NAME HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')='generation,tenant_id' AND MIN(NON_UNIQUE)=0 AND MAX(NON_UNIQUE)=0 AND SUM(SUB_PART IS NOT NULL)=0 AND MIN(INDEX_TYPE)='BTREE' AND MAX(INDEX_TYPE)='BTREE' AND MIN(IS_VISIBLE)='YES' AND MAX(IS_VISIBLE)='YES')
 OR (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement')!=5 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 retention exact schema mismatch';
 END IF;
 IF (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND CONSTRAINT_TYPE='CHECK')!=4
 OR (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS tc
     JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
     JOIN (
       SELECT 'ck_wb_retirement_scope' AS constraint_name, '(`tenant_id` >= 0)' AS check_clause
       UNION ALL SELECT 'ck_wb_retirement_state',CONVERT(0x286073746174656020696e20285f757466386d62345c27524554495245445c272c5f757466386d62345c275055524745445c272929 USING utf8mb4)
       UNION ALL SELECT 'ck_wb_retirement_window','((`retired_at_millis` >= 0) and (`purge_not_before_millis` > `retired_at_millis`))'
       UNION ALL SELECT 'ck_wb_retirement_duration','((`retention_seconds` > 0) and (`rollback_protection_seconds` > 0))'
     ) expected ON CAST(tc.CONSTRAINT_NAME AS BINARY)=CAST(expected.constraint_name AS BINARY)
       AND CAST(cc.CHECK_CLAUSE AS BINARY)=CAST(expected.check_clause AS BINARY)
     WHERE tc.CONSTRAINT_SCHEMA=DATABASE() AND tc.TABLE_SCHEMA=DATABASE()
       AND CAST(tc.TABLE_NAME AS BINARY)=CAST('system_workbench_generation_retirement' AS BINARY)
       AND tc.CONSTRAINT_TYPE='CHECK' AND tc.ENFORCED='YES')!=4 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 retention exact enforced CHECK contract mismatch';
 END IF;
END$$
CALL wb_s4_retention_check_20261023()$$
DROP PROCEDURE wb_s4_retention_check_20261023$$
DELIMITER ;

-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261023_system_workbench_retention_check_identity; supersedes=20261011_system_workbench_projection_history; type=schema; riskLevel=high; applyOrder=10; requiresTargetPreflight=true
-- Candidate production migration; not applied by this static delivery.
-- Five empty additive tables; enable only after explicitly approved maintenance.
-- Adds five empty protocol tables only: no ALTER, payload cleanup, seed, backfill or activation.
-- MySQL 8.0 exact metadata contract follows S4 guards; ASCII CHECK literals use explicit _ascii.
-- CHECK metadata byte contract verified against MySQL 8.0.39; original DDL is unchanged.
DELIMITER $$
CREATE PROCEDURE wb_s4_history_check_20261023()
SQL SECURITY INVOKER
BEGIN
 IF DATABASE() IS NULL OR COALESCE(@profile_workbench_s4_maintenance,0)!=1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 history approved maintenance prerequisite missing';
 END IF;
 IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
     AND TABLE_NAME IN ('system_workbench_global_control','system_workbench_tenant_control','system_workbench_outbox','system_workbench_generation_global','system_workbench_generation','system_workbench_task_projection','system_workbench_task_audience','system_workbench_authorization_projection','system_workbench_consumed_event','system_workbench_generation_retirement')
     AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=10 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 history projection and retirement prerequisites missing';
 END IF;
 CREATE TABLE IF NOT EXISTS system_workbench_scope_floor (
  scope varchar(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tenant_id bigint NOT NULL,
  payload_floor bigint NOT NULL,
  PRIMARY KEY(scope,tenant_id),
  CONSTRAINT ck_wb_history_floor_scope CHECK ((`scope` in (_ascii'GLOBAL',_ascii'TENANT')) and (`tenant_id` >= 0) and ((`scope` <> _ascii'GLOBAL') or (`tenant_id` = 0))),
  CONSTRAINT ck_wb_history_floor_value CHECK (`payload_floor` >= 0)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
 CREATE TABLE IF NOT EXISTS system_workbench_batch_identity (
  scope varchar(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tenant_id bigint NOT NULL,
  commit_sequence bigint NOT NULL,
  batch_key varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  expected_count int NOT NULL,
  schema_version int NOT NULL,
  batch_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  observed_at_millis bigint NULL,
  purge_not_before_millis bigint NULL,
  retention_seconds bigint NULL,
  rollback_seconds bigint NULL,
  clock_skew_seconds bigint NULL,
  approval_reference varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
  PRIMARY KEY(scope,tenant_id,commit_sequence),
  UNIQUE KEY uk_wb_history_batch_key(scope,tenant_id,batch_key),
  CONSTRAINT ck_wb_history_batch_scope CHECK ((`scope` in (_ascii'GLOBAL',_ascii'TENANT')) and (`tenant_id` >= 0) and ((`scope` <> _ascii'GLOBAL') or (`tenant_id` = 0))),
  CONSTRAINT ck_wb_history_batch_identity CHECK ((`commit_sequence` > 0) and (`expected_count` > 0) and (`schema_version` = 4)),
  CONSTRAINT ck_wb_history_batch_observation CHECK (((`observed_at_millis` is null) and (`purge_not_before_millis` is null) and (`retention_seconds` is null) and (`rollback_seconds` is null) and (`clock_skew_seconds` is null) and (`approval_reference` is null)) or ((`observed_at_millis` is not null) and (`purge_not_before_millis` is not null) and (`retention_seconds` is not null) and (`rollback_seconds` is not null) and (`clock_skew_seconds` is not null) and (`approval_reference` is not null))),
  CONSTRAINT ck_wb_history_batch_window CHECK ((`observed_at_millis` is null) or ((`observed_at_millis` >= 0) and (`purge_not_before_millis` > `observed_at_millis`) and (`retention_seconds` > 0) and (`rollback_seconds` > 0) and (`clock_skew_seconds` >= 0)))
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
 CREATE TABLE IF NOT EXISTS system_workbench_event_identity (
  event_id varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  scope varchar(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tenant_id bigint NOT NULL,
  commit_sequence bigint NOT NULL,
  batch_key varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  event_ordinal int NOT NULL,
  expected_count int NOT NULL,
  schema_version int NOT NULL,
  content_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY(event_id),
  UNIQUE KEY uk_wb_history_event_ordinal(scope,tenant_id,batch_key,event_ordinal),
  KEY idx_wb_history_event_commit(scope,tenant_id,commit_sequence,event_ordinal),
  CONSTRAINT ck_wb_history_event_scope CHECK ((`scope` in (_ascii'GLOBAL',_ascii'TENANT')) and (`tenant_id` >= 0) and ((`scope` <> _ascii'GLOBAL') or (`tenant_id` = 0))),
  CONSTRAINT ck_wb_history_event_identity CHECK ((`commit_sequence` > 0) and (`event_ordinal` >= 0) and (`expected_count` > `event_ordinal`) and (`schema_version` = 4))
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
 CREATE TABLE IF NOT EXISTS system_workbench_task_version_identity (
  generation varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tenant_id bigint NOT NULL,
  task_key varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
  entity_version bigint NOT NULL,
  source_id varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  content_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  closed_version_to bigint NULL,
  observed_at_millis bigint NULL,
  purge_not_before_millis bigint NULL,
  retention_seconds bigint NULL,
  rollback_seconds bigint NULL,
  clock_skew_seconds bigint NULL,
  approval_reference varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
  PRIMARY KEY(generation,tenant_id,task_key,entity_version),
  KEY idx_wb_history_task_window(generation,tenant_id,purge_not_before_millis),
  CONSTRAINT ck_wb_history_task_identity CHECK ((`tenant_id` >= 0) and (`entity_version` > 0)),
  CONSTRAINT ck_wb_history_task_observation CHECK (((`closed_version_to` is null) and (`observed_at_millis` is null) and (`purge_not_before_millis` is null) and (`retention_seconds` is null) and (`rollback_seconds` is null) and (`clock_skew_seconds` is null) and (`approval_reference` is null)) or ((`closed_version_to` is not null) and (`observed_at_millis` is not null) and (`purge_not_before_millis` is not null) and (`retention_seconds` is not null) and (`rollback_seconds` is not null) and (`clock_skew_seconds` is not null) and (`approval_reference` is not null))),
  CONSTRAINT ck_wb_history_task_window CHECK ((`observed_at_millis` is null) or ((`closed_version_to` > `entity_version`) and (`observed_at_millis` >= 0) and (`purge_not_before_millis` > `observed_at_millis`) and (`retention_seconds` > 0) and (`rollback_seconds` > 0) and (`clock_skew_seconds` >= 0)))
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
 CREATE TABLE IF NOT EXISTS system_workbench_auth_version_identity (
  generation varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tenant_id bigint NOT NULL,
  source_id varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  user_id bigint NOT NULL,
  task_key varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
  policy_id varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  grant_key varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  entity_version bigint NOT NULL,
  content_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  closed_version_to bigint NULL,
  observed_at_millis bigint NULL,
  purge_not_before_millis bigint NULL,
  retention_seconds bigint NULL,
  rollback_seconds bigint NULL,
  clock_skew_seconds bigint NULL,
  approval_reference varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
  PRIMARY KEY(generation,tenant_id,source_id,user_id,task_key,policy_id,grant_key,entity_version),
  KEY idx_wb_history_auth_window(generation,tenant_id,purge_not_before_millis),
  CONSTRAINT ck_wb_history_auth_identity CHECK ((`tenant_id` >= 0) and (`user_id` > 0) and (`entity_version` > 0)),
  CONSTRAINT ck_wb_history_auth_observation CHECK (((`closed_version_to` is null) and (`observed_at_millis` is null) and (`purge_not_before_millis` is null) and (`retention_seconds` is null) and (`rollback_seconds` is null) and (`clock_skew_seconds` is null) and (`approval_reference` is null)) or ((`closed_version_to` is not null) and (`observed_at_millis` is not null) and (`purge_not_before_millis` is not null) and (`retention_seconds` is not null) and (`rollback_seconds` is not null) and (`clock_skew_seconds` is not null) and (`approval_reference` is not null))),
  CONSTRAINT ck_wb_history_auth_window CHECK ((`observed_at_millis` is null) or ((`closed_version_to` > `entity_version`) and (`observed_at_millis` >= 0) and (`purge_not_before_millis` > `observed_at_millis`) and (`retention_seconds` > 0) and (`rollback_seconds` > 0) and (`clock_skew_seconds` >= 0)))
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
 -- Exact column set, order, type, nullability, collation, default and EXTRA: no automatic repair.
 IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
     AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity') AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=5
 OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))!=54
 OR (SELECT COUNT(*) FROM information_schema.COLUMNS c
     JOIN (
       SELECT 'system_workbench_scope_floor' AS table_name,'scope' AS column_name,'varchar(8)' AS column_type,'NO' AS nullable,1 AS ordinal,'ascii_bin' AS collation_name
       UNION ALL SELECT 'system_workbench_scope_floor','tenant_id','bigint','NO',2,NULL
       UNION ALL SELECT 'system_workbench_scope_floor','payload_floor','bigint','NO',3,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','scope','varchar(8)','NO',1,'ascii_bin'
       UNION ALL SELECT 'system_workbench_batch_identity','tenant_id','bigint','NO',2,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','commit_sequence','bigint','NO',3,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','batch_key','varchar(128)','NO',4,'ascii_bin'
       UNION ALL SELECT 'system_workbench_batch_identity','expected_count','int','NO',5,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','schema_version','int','NO',6,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','batch_hash','char(64)','NO',7,'ascii_bin'
       UNION ALL SELECT 'system_workbench_batch_identity','observed_at_millis','bigint','YES',8,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','purge_not_before_millis','bigint','YES',9,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','retention_seconds','bigint','YES',10,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','rollback_seconds','bigint','YES',11,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','clock_skew_seconds','bigint','YES',12,NULL
       UNION ALL SELECT 'system_workbench_batch_identity','approval_reference','varchar(128)','YES',13,'utf8mb4_0900_bin'
       UNION ALL SELECT 'system_workbench_event_identity','event_id','varchar(64)','NO',1,'ascii_bin'
       UNION ALL SELECT 'system_workbench_event_identity','scope','varchar(8)','NO',2,'ascii_bin'
       UNION ALL SELECT 'system_workbench_event_identity','tenant_id','bigint','NO',3,NULL
       UNION ALL SELECT 'system_workbench_event_identity','commit_sequence','bigint','NO',4,NULL
       UNION ALL SELECT 'system_workbench_event_identity','batch_key','varchar(128)','NO',5,'ascii_bin'
       UNION ALL SELECT 'system_workbench_event_identity','event_ordinal','int','NO',6,NULL
       UNION ALL SELECT 'system_workbench_event_identity','expected_count','int','NO',7,NULL
       UNION ALL SELECT 'system_workbench_event_identity','schema_version','int','NO',8,NULL
       UNION ALL SELECT 'system_workbench_event_identity','content_hash','char(64)','NO',9,'ascii_bin'
       UNION ALL SELECT 'system_workbench_task_version_identity','generation','varchar(64)','NO',1,'ascii_bin'
       UNION ALL SELECT 'system_workbench_task_version_identity','tenant_id','bigint','NO',2,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','task_key','varchar(160)','NO',3,'utf8mb4_0900_bin'
       UNION ALL SELECT 'system_workbench_task_version_identity','entity_version','bigint','NO',4,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','source_id','varchar(64)','NO',5,'ascii_bin'
       UNION ALL SELECT 'system_workbench_task_version_identity','content_hash','char(64)','NO',6,'ascii_bin'
       UNION ALL SELECT 'system_workbench_task_version_identity','closed_version_to','bigint','YES',7,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','observed_at_millis','bigint','YES',8,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','purge_not_before_millis','bigint','YES',9,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','retention_seconds','bigint','YES',10,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','rollback_seconds','bigint','YES',11,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','clock_skew_seconds','bigint','YES',12,NULL
       UNION ALL SELECT 'system_workbench_task_version_identity','approval_reference','varchar(128)','YES',13,'utf8mb4_0900_bin'
       UNION ALL SELECT 'system_workbench_auth_version_identity','generation','varchar(64)','NO',1,'ascii_bin'
       UNION ALL SELECT 'system_workbench_auth_version_identity','tenant_id','bigint','NO',2,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','source_id','varchar(64)','NO',3,'ascii_bin'
       UNION ALL SELECT 'system_workbench_auth_version_identity','user_id','bigint','NO',4,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','task_key','varchar(160)','NO',5,'utf8mb4_0900_bin'
       UNION ALL SELECT 'system_workbench_auth_version_identity','policy_id','varchar(128)','NO',6,'ascii_bin'
       UNION ALL SELECT 'system_workbench_auth_version_identity','grant_key','varchar(64)','NO',7,'ascii_bin'
       UNION ALL SELECT 'system_workbench_auth_version_identity','entity_version','bigint','NO',8,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','content_hash','char(64)','NO',9,'ascii_bin'
       UNION ALL SELECT 'system_workbench_auth_version_identity','closed_version_to','bigint','YES',10,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','observed_at_millis','bigint','YES',11,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','purge_not_before_millis','bigint','YES',12,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','retention_seconds','bigint','YES',13,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','rollback_seconds','bigint','YES',14,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','clock_skew_seconds','bigint','YES',15,NULL
       UNION ALL SELECT 'system_workbench_auth_version_identity','approval_reference','varchar(128)','YES',16,'utf8mb4_0900_bin'
     ) expected ON CAST(c.TABLE_NAME AS BINARY)=CAST(expected.table_name AS BINARY)
       AND CAST(c.COLUMN_NAME AS BINARY)=CAST(expected.column_name AS BINARY)
       AND CAST(c.COLUMN_TYPE AS BINARY)=CAST(expected.column_type AS BINARY)
       AND CAST(c.IS_NULLABLE AS BINARY)=CAST(expected.nullable AS BINARY)
       AND c.ORDINAL_POSITION=expected.ordinal
       AND (CAST(c.COLLATION_NAME AS BINARY) <=> CAST(expected.collation_name AS BINARY))
     WHERE c.TABLE_SCHEMA=DATABASE() AND c.COLUMN_DEFAULT IS NULL AND c.EXTRA='' AND c.COLUMN_COMMENT='')!=54 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 history exact column contract mismatch';
 END IF;
 -- PK/unique/ordinary index sets, ordered full columns and visibility are exact.
 IF (SELECT COUNT(*) FROM (SELECT TABLE_NAME,INDEX_NAME FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity') GROUP BY TABLE_NAME,INDEX_NAME) actual_names)!=10
 OR (SELECT COUNT(*) FROM (
       SELECT TABLE_NAME,INDEX_NAME,GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS columns_ordered,
         COUNT(*) AS columns_count,MIN(NON_UNIQUE) AS min_non_unique,MAX(NON_UNIQUE) AS max_non_unique,
         SUM(SUB_PART IS NOT NULL) AS prefix_count,MIN(INDEX_TYPE) AS min_type,MAX(INDEX_TYPE) AS max_type,
         MIN(IS_VISIBLE) AS min_visible,MAX(IS_VISIBLE) AS max_visible,
         SUM(COLLATION IS NULL OR COLLATION!='A') AS wrong_direction,SUM(EXPRESSION IS NOT NULL) AS expression_count
       FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity')
       GROUP BY TABLE_NAME,INDEX_NAME
     ) actual JOIN (
       SELECT 'system_workbench_scope_floor' AS table_name,'PRIMARY' AS index_name,'scope,tenant_id' AS columns_ordered,0 AS non_unique,2 AS columns_count
       UNION ALL SELECT 'system_workbench_batch_identity','PRIMARY','scope,tenant_id,commit_sequence',0,3
       UNION ALL SELECT 'system_workbench_batch_identity','uk_wb_history_batch_key','scope,tenant_id,batch_key',0,3
       UNION ALL SELECT 'system_workbench_event_identity','PRIMARY','event_id',0,1
       UNION ALL SELECT 'system_workbench_event_identity','uk_wb_history_event_ordinal','scope,tenant_id,batch_key,event_ordinal',0,4
       UNION ALL SELECT 'system_workbench_event_identity','idx_wb_history_event_commit','scope,tenant_id,commit_sequence,event_ordinal',1,4
       UNION ALL SELECT 'system_workbench_task_version_identity','PRIMARY','generation,tenant_id,task_key,entity_version',0,4
       UNION ALL SELECT 'system_workbench_task_version_identity','idx_wb_history_task_window','generation,tenant_id,purge_not_before_millis',1,3
       UNION ALL SELECT 'system_workbench_auth_version_identity','PRIMARY','generation,tenant_id,source_id,user_id,task_key,policy_id,grant_key,entity_version',0,8
       UNION ALL SELECT 'system_workbench_auth_version_identity','idx_wb_history_auth_window','generation,tenant_id,purge_not_before_millis',1,3
     ) expected ON CAST(actual.TABLE_NAME AS BINARY)=CAST(expected.table_name AS BINARY)
       AND CAST(actual.INDEX_NAME AS BINARY)=CAST(expected.index_name AS BINARY)
       AND CAST(actual.columns_ordered AS BINARY)=CAST(expected.columns_ordered AS BINARY)
       AND actual.columns_count=expected.columns_count
       AND actual.min_non_unique=expected.non_unique AND actual.max_non_unique=expected.non_unique
       AND actual.prefix_count=0 AND actual.min_type='BTREE' AND actual.max_type='BTREE'
       AND actual.min_visible='YES' AND actual.max_visible='YES'
       AND actual.wrong_direction=0 AND actual.expression_count=0)!=10
 OR (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()
     AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))!=21 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 history exact index and constraint set mismatch';
 END IF;
 -- Four CHECK classes: scope, identity, observation all-null/all-present, positive protected window.
 -- Compare complete MySQL CHECK_CLAUSE bytes, never normalize whitespace/parentheses/business literals.
 -- Hex constants encode verified metadata backslashes independently of session SQL_MODE.
 IF (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()
     AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity') AND CONSTRAINT_TYPE='CHECK')!=14
 OR (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS tc
     JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
     JOIN (
       SELECT 'system_workbench_scope_floor' AS table_name,'ck_wb_history_floor_scope' AS constraint_name,CONVERT(0x28286073636f70656020696e20285f61736369695c27474c4f42414c5c272c5f61736369695c2754454e414e545c27292920616e6420286074656e616e745f696460203e3d20302920616e642028286073636f706560203c3e205f61736369695c27474c4f42414c5c2729206f7220286074656e616e745f696460203d2030292929 USING utf8mb4) AS check_clause
       UNION ALL SELECT 'system_workbench_scope_floor','ck_wb_history_floor_value','(`payload_floor` >= 0)'
       UNION ALL SELECT 'system_workbench_batch_identity','ck_wb_history_batch_scope',CONVERT(0x28286073636f70656020696e20285f61736369695c27474c4f42414c5c272c5f61736369695c2754454e414e545c27292920616e6420286074656e616e745f696460203e3d20302920616e642028286073636f706560203c3e205f61736369695c27474c4f42414c5c2729206f7220286074656e616e745f696460203d2030292929 USING utf8mb4)
       UNION ALL SELECT 'system_workbench_batch_identity','ck_wb_history_batch_identity','((`commit_sequence` > 0) and (`expected_count` > 0) and (`schema_version` = 4))'
       UNION ALL SELECT 'system_workbench_batch_identity','ck_wb_history_batch_observation','(((`observed_at_millis` is null) and (`purge_not_before_millis` is null) and (`retention_seconds` is null) and (`rollback_seconds` is null) and (`clock_skew_seconds` is null) and (`approval_reference` is null)) or ((`observed_at_millis` is not null) and (`purge_not_before_millis` is not null) and (`retention_seconds` is not null) and (`rollback_seconds` is not null) and (`clock_skew_seconds` is not null) and (`approval_reference` is not null)))'
       UNION ALL SELECT 'system_workbench_batch_identity','ck_wb_history_batch_window','((`observed_at_millis` is null) or ((`observed_at_millis` >= 0) and (`purge_not_before_millis` > `observed_at_millis`) and (`retention_seconds` > 0) and (`rollback_seconds` > 0) and (`clock_skew_seconds` >= 0)))'
       UNION ALL SELECT 'system_workbench_event_identity','ck_wb_history_event_scope',CONVERT(0x28286073636f70656020696e20285f61736369695c27474c4f42414c5c272c5f61736369695c2754454e414e545c27292920616e6420286074656e616e745f696460203e3d20302920616e642028286073636f706560203c3e205f61736369695c27474c4f42414c5c2729206f7220286074656e616e745f696460203d2030292929 USING utf8mb4)
       UNION ALL SELECT 'system_workbench_event_identity','ck_wb_history_event_identity','((`commit_sequence` > 0) and (`event_ordinal` >= 0) and (`expected_count` > `event_ordinal`) and (`schema_version` = 4))'
       UNION ALL SELECT 'system_workbench_task_version_identity','ck_wb_history_task_identity','((`tenant_id` >= 0) and (`entity_version` > 0))'
       UNION ALL SELECT 'system_workbench_task_version_identity','ck_wb_history_task_observation','(((`closed_version_to` is null) and (`observed_at_millis` is null) and (`purge_not_before_millis` is null) and (`retention_seconds` is null) and (`rollback_seconds` is null) and (`clock_skew_seconds` is null) and (`approval_reference` is null)) or ((`closed_version_to` is not null) and (`observed_at_millis` is not null) and (`purge_not_before_millis` is not null) and (`retention_seconds` is not null) and (`rollback_seconds` is not null) and (`clock_skew_seconds` is not null) and (`approval_reference` is not null)))'
       UNION ALL SELECT 'system_workbench_task_version_identity','ck_wb_history_task_window','((`observed_at_millis` is null) or ((`closed_version_to` > `entity_version`) and (`observed_at_millis` >= 0) and (`purge_not_before_millis` > `observed_at_millis`) and (`retention_seconds` > 0) and (`rollback_seconds` > 0) and (`clock_skew_seconds` >= 0)))'
       UNION ALL SELECT 'system_workbench_auth_version_identity','ck_wb_history_auth_identity','((`tenant_id` >= 0) and (`user_id` > 0) and (`entity_version` > 0))'
       UNION ALL SELECT 'system_workbench_auth_version_identity','ck_wb_history_auth_observation','(((`closed_version_to` is null) and (`observed_at_millis` is null) and (`purge_not_before_millis` is null) and (`retention_seconds` is null) and (`rollback_seconds` is null) and (`clock_skew_seconds` is null) and (`approval_reference` is null)) or ((`closed_version_to` is not null) and (`observed_at_millis` is not null) and (`purge_not_before_millis` is not null) and (`retention_seconds` is not null) and (`rollback_seconds` is not null) and (`clock_skew_seconds` is not null) and (`approval_reference` is not null)))'
       UNION ALL SELECT 'system_workbench_auth_version_identity','ck_wb_history_auth_window','((`observed_at_millis` is null) or ((`closed_version_to` > `entity_version`) and (`observed_at_millis` >= 0) and (`purge_not_before_millis` > `observed_at_millis`) and (`retention_seconds` > 0) and (`rollback_seconds` > 0) and (`clock_skew_seconds` >= 0)))'
     ) expected ON CAST(tc.TABLE_NAME AS BINARY)=CAST(expected.table_name AS BINARY)
       AND CAST(tc.CONSTRAINT_NAME AS BINARY)=CAST(expected.constraint_name AS BINARY)
       AND CAST(cc.CHECK_CLAUSE AS BINARY)=CAST(expected.check_clause AS BINARY)
     WHERE tc.CONSTRAINT_SCHEMA=DATABASE() AND tc.TABLE_SCHEMA=DATABASE()
       AND tc.CONSTRAINT_TYPE='CHECK' AND tc.ENFORCED='YES')!=14 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S4 history exact enforced CHECK contract mismatch';
 END IF;
END$$
-- Execution requires approved maintenance and the complete dependency closure.
CALL wb_s4_history_check_20261023()$$
DROP PROCEDURE wb_s4_history_check_20261023$$
DELIMITER ;

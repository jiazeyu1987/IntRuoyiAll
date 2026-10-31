-- release-target-preflight: migrationId=20261023_system_workbench_history_check_identity; allowedEnvironments=test,backup,prod
-- Existing target tables must satisfy the exact contract; absent tables are created by this migration.
-- Maintenance authorization remains an execution-only runtime guard, never self-assigned here.
SELECT CASE WHEN
 NOT ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
     AND TABLE_NAME IN ('system_workbench_global_control','system_workbench_tenant_control','system_workbench_outbox','system_workbench_generation_global','system_workbench_generation','system_workbench_task_projection','system_workbench_task_audience','system_workbench_authorization_projection','system_workbench_consumed_event','system_workbench_generation_retirement')
     AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=10)
 AND NOT ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
     AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity') AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 1 WHEN 'system_workbench_batch_identity' THEN 1 WHEN 'system_workbench_event_identity' THEN 1 WHEN 'system_workbench_task_version_identity' THEN 1 WHEN 'system_workbench_auth_version_identity' THEN 1 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))
 OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 3 WHEN 'system_workbench_batch_identity' THEN 13 WHEN 'system_workbench_event_identity' THEN 9 WHEN 'system_workbench_task_version_identity' THEN 13 WHEN 'system_workbench_auth_version_identity' THEN 16 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))
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
     WHERE c.TABLE_SCHEMA=DATABASE() AND c.COLUMN_DEFAULT IS NULL AND c.EXTRA='' AND c.COLUMN_COMMENT='')!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 3 WHEN 'system_workbench_batch_identity' THEN 13 WHEN 'system_workbench_event_identity' THEN 9 WHEN 'system_workbench_task_version_identity' THEN 13 WHEN 'system_workbench_auth_version_identity' THEN 16 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity')))
 AND NOT ((SELECT COUNT(*) FROM (SELECT TABLE_NAME,INDEX_NAME FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity') GROUP BY TABLE_NAME,INDEX_NAME) actual_names)!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 1 WHEN 'system_workbench_batch_identity' THEN 2 WHEN 'system_workbench_event_identity' THEN 3 WHEN 'system_workbench_task_version_identity' THEN 2 WHEN 'system_workbench_auth_version_identity' THEN 2 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))
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
       AND actual.wrong_direction=0 AND actual.expression_count=0)!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 1 WHEN 'system_workbench_batch_identity' THEN 2 WHEN 'system_workbench_event_identity' THEN 3 WHEN 'system_workbench_task_version_identity' THEN 2 WHEN 'system_workbench_auth_version_identity' THEN 2 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))
 OR (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()
     AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 3 WHEN 'system_workbench_batch_identity' THEN 6 WHEN 'system_workbench_event_identity' THEN 4 WHEN 'system_workbench_task_version_identity' THEN 4 WHEN 'system_workbench_auth_version_identity' THEN 4 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity')))
 AND NOT ((SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()
     AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity') AND CONSTRAINT_TYPE='CHECK')!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 2 WHEN 'system_workbench_batch_identity' THEN 4 WHEN 'system_workbench_event_identity' THEN 2 WHEN 'system_workbench_task_version_identity' THEN 3 WHEN 'system_workbench_auth_version_identity' THEN 3 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity'))
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
       AND tc.CONSTRAINT_TYPE='CHECK' AND tc.ENFORCED='YES')!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_scope_floor' THEN 2 WHEN 'system_workbench_batch_identity' THEN 4 WHEN 'system_workbench_event_identity' THEN 2 WHEN 'system_workbench_task_version_identity' THEN 3 WHEN 'system_workbench_auth_version_identity' THEN 3 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_scope_floor','system_workbench_batch_identity','system_workbench_event_identity','system_workbench_task_version_identity','system_workbench_auth_version_identity')))
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN('wb_s4_history_20261011','wb_s4_history_check_20261023'))
 THEN 'TARGET_PREFLIGHT_PASS:20261023_system_workbench_history_check_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_system_workbench_history_check_identity:S4_SCHEMA_PREREQUISITE_OR_IDENTITY' END;

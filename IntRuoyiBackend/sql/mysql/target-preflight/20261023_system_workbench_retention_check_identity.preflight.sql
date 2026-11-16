-- release-target-preflight: migrationId=20261023_system_workbench_retention_check_identity; allowedEnvironments=test,backup,prod
-- Existing target tables must satisfy the exact contract; absent tables are created by this migration.
-- Maintenance authorization remains an execution-only runtime guard, never self-assigned here.
SELECT CASE WHEN
 NOT ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
     AND TABLE_NAME IN ('system_workbench_global_control','system_workbench_tenant_control','system_workbench_outbox','system_workbench_generation_global','system_workbench_generation','system_workbench_task_projection','system_workbench_task_audience','system_workbench_authorization_projection','system_workbench_consumed_event')
     AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=9)
 AND (NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement') OR NOT ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_0900_bin')!=1
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
 OR (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement')!=5))
 AND NOT ((SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_workbench_generation_retirement' AND CONSTRAINT_TYPE='CHECK')!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_generation_retirement' THEN 4 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_generation_retirement'))
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
       AND tc.CONSTRAINT_TYPE='CHECK' AND tc.ENFORCED='YES')!=(SELECT COALESCE(SUM(CASE TABLE_NAME WHEN 'system_workbench_generation_retirement' THEN 4 END),0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_workbench_generation_retirement')))
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN('wb_s4_retention_schema_20261011','wb_s4_retention_check_20261023'))
 THEN 'TARGET_PREFLIGHT_PASS:20261023_system_workbench_retention_check_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_system_workbench_retention_check_identity:S4_SCHEMA_PREREQUISITE_OR_IDENTITY' END;

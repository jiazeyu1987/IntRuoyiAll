SELECT 'database_bytes', SUM(DATA_LENGTH+INDEX_LENGTH), COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();
SELECT 'core_rows', 'dcc_controlled_file', COUNT(*) FROM dcc_controlled_file;
SELECT 'core_rows', 'dcc_controlled_file_master', COUNT(*) FROM dcc_controlled_file_master;
SELECT 'core_rows', 'dcc_controlled_file_signature', COUNT(*) FROM dcc_controlled_file_signature;
SELECT 'schema', TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,EXTRA FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND (TABLE_NAME LIKE 'dcc_%' OR TABLE_NAME LIKE 'bpm_%'
 OR TABLE_NAME LIKE 'gxp_%' OR TABLE_NAME IN ('infra_file','system_notify_template','system_notify_message','infra_job','infra_release_migration'))
 ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT 'flowable_table',TABLE_NAME FROM information_schema.TABLES
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'ACT_%' ORDER BY TABLE_NAME;
SELECT 'source_duplicates',COUNT(*) FROM (
 SELECT tenant_id,master_id,version_no,COUNT(*) AS n FROM dcc_controlled_file
 WHERE deleted=0 GROUP BY tenant_id,master_id,version_no HAVING COUNT(*)>1
) AS duplicate_groups;

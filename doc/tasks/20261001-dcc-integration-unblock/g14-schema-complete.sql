SELECT 'schema', TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,EXTRA,GENERATION_EXPRESSION
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
 AND (TABLE_NAME LIKE 'dcc_%' OR TABLE_NAME LIKE 'bpm_%' OR TABLE_NAME LIKE 'act_%'
 OR TABLE_NAME LIKE 'gxp_%' OR TABLE_NAME LIKE 'signature_%'
 OR TABLE_NAME IN ('system_notify_template','system_notify_message','system_config','infra_job','infra_release_migration'))
 ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT 'index',TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART
 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
 AND (TABLE_NAME LIKE 'dcc_%' OR TABLE_NAME LIKE 'bpm_%' OR TABLE_NAME LIKE 'act_%'
 OR TABLE_NAME LIKE 'gxp_%' OR TABLE_NAME LIKE 'signature_%'
 OR TABLE_NAME IN ('system_notify_template','system_notify_message','infra_job','infra_release_migration'))
 ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT 'size',TABLE_NAME,TABLE_ROWS,DATA_LENGTH,INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA=DATABASE() AND (TABLE_NAME LIKE 'dcc_%' OR TABLE_NAME LIKE 'bpm_%'
 OR TABLE_NAME LIKE 'act_%' OR TABLE_NAME LIKE 'gxp_%' OR TABLE_NAME LIKE 'signature_%'
 OR TABLE_NAME IN ('infra_file','system_notify_template','system_notify_message','system_menu','system_tenant',
 'system_tenant_package','system_user','system_role','system_dept','system_user_role','infra_job','infra_release_migration'))
 ORDER BY TABLE_NAME;

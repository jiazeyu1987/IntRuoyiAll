-- READ ONLY. Fresh target/source/hash and actual quality actor checks before any config authorization.
SELECT DATABASE() AS actual_database,@@hostname AS actual_host,@@port AS actual_server_port;
SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
 AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','system_users');
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,COLLATION_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','system_users')
 ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version')
 ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT id,tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,
 signature_policy,state_policy,retention_class,test_ids,owner,applicability,HEX(active) AS active_hex,HEX(deleted) AS deleted_hex,
 create_time,update_time,creator,updater FROM gxp_audit_policy_operation ORDER BY tenant_id,operation_id,policy_version,id;
SELECT id,tenant_id,policy_version,policy_hash,approved_by,approved_at,approval_reference,coverage_report_hash,create_time
 FROM gxp_audit_policy_version ORDER BY tenant_id,policy_version,id;
-- Provide the actual externally-confirmed quality account ID; no account/admin assumption.
SELECT id,tenant_id,username,nickname,status,HEX(deleted) AS deleted_hex FROM system_users
 WHERE tenant_id=1 AND id=@dcc_gxp_quality_approved_by;

-- READONLY actual source capture. Root keeps full raw rows and all original metadata.
SELECT DATABASE() AS database_name,@@server_uuid AS server_uuid,@@version AS mysql_version,@@session.time_zone AS time_zone,@@character_set_connection AS connection_charset,@@collation_connection AS connection_collation,@@session.sql_mode AS sql_mode;
SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation');
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLLATION_NAME,ORDINAL_POSITION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' ORDER BY ORDINAL_POSITION;
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' ORDER BY INDEX_NAME,SEQ_IN_INDEX;
SELECT * FROM gxp_audit_policy_operation ORDER BY id;
SELECT * FROM gxp_audit_policy_version ORDER BY id;
SELECT migration_id,sha256,status,target_environment,deleted+0 AS deleted FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy';
SELECT ROUTINE_NAME,ROUTINE_TYPE,CREATED,LAST_ALTERED FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='dcc_g43_dev26_config';
SELECT TRIGGER_NAME,EVENT_MANIPULATION,ACTION_TIMING FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE='gxp_audit_policy_operation';

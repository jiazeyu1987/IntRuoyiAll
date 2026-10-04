-- SELECT only. New legacy sidecar DDL/config remains unapproved and unexecuted.
SELECT JSON_OBJECT('kind','runtime','database',DATABASE(),'serverUuid',@@server_uuid,
 'mysqlVersion',@@version,'innodbPageSize',@@innodb_page_size,
 'innodbDefaultRowFormat',@@innodb_default_row_format,'sessionSqlMode',@@session.sql_mode,
 'capturedAtUtc',UTC_TIMESTAMP(6));
SELECT JSON_OBJECT('kind','new_target_presence','tableName',TABLE_NAME,'engine',ENGINE,
 'collation',TABLE_COLLATION,'rowFormat',ROW_FORMAT)
FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
 AND TABLE_NAME IN ('dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation')
ORDER BY TABLE_NAME;
SELECT JSON_OBJECT('kind','target_ledger_presence','targetRows',COUNT(*))
FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy';

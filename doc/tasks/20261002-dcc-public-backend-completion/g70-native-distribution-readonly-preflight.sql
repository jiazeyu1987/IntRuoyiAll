-- Root-only read collector. No credentials, DML or routine invocation.
SELECT DATABASE() AS database_name, @@server_uuid AS database_uuid, @@version AS mysql_version,
       @@sql_mode AS sql_mode, @@character_set_connection AS connection_charset;
SELECT id,name,permission,type,sort,parent_id,path,icon,component,component_name,
       status,visible,keep_alive,always_show,deleted
FROM system_menu WHERE id IN (6800,6808,6819,6839)
   OR permission = 'dcc:controlled-file:distribute' ORDER BY id;
SELECT id,tenant_id,code,status,deleted FROM system_role
WHERE id = 910233 OR (tenant_id = 1 AND code = 'doc_control') ORDER BY id;
SELECT * FROM system_role_menu WHERE role_id = 910233
   OR menu_id = 6839
   OR menu_id IN (SELECT id FROM system_menu WHERE permission = 'dcc:controlled-file:distribute') ORDER BY id;
SELECT ROUTINE_SCHEMA,ROUTINE_NAME,ROUTINE_TYPE FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_NAME = 'g70_dcc_native_distribute';
-- Capture all old rows of the following four tables to protected Root baseline, not stdout guesses:
SELECT * FROM system_menu ORDER BY id;
SELECT * FROM system_role_menu ORDER BY id;
SELECT * FROM system_role ORDER BY id;
SELECT * FROM system_tenant_package ORDER BY id;

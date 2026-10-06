-- Same Root connection/source identity after execution; no DML. Original four-table rows must match byte-for-byte.
SELECT DATABASE() AS database_name, @@server_uuid AS database_uuid, @@version AS mysql_version;
SELECT id,name,permission,type,sort,parent_id,path,icon,component,component_name,
       status,visible,keep_alive,always_show,deleted
FROM system_menu WHERE permission = 'dcc:controlled-file:distribute' ORDER BY id;
SELECT rm.* FROM system_role_menu rm JOIN system_menu m ON m.id=rm.menu_id
WHERE rm.role_id=910233 AND m.permission='dcc:controlled-file:distribute' ORDER BY rm.id;
SELECT ROUTINE_SCHEMA,ROUTINE_NAME,ROUTINE_TYPE FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_NAME = 'g70_dcc_native_distribute';
SELECT * FROM system_menu ORDER BY id;
SELECT * FROM system_role_menu ORDER BY id;
SELECT * FROM system_role ORDER BY id;
SELECT * FROM system_tenant_package ORDER BY id;

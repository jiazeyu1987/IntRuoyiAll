-- release-target-preflight: migrationId=20260922_intern_user_time_maintenance_module; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('system_role','system_menu','system_role_menu','system_tenant'))=4)
 AND ((SELECT COUNT(*) FROM system_tenant WHERE id=1 AND deleted=0 AND status=0)=1)
 AND (NOT EXISTS(SELECT 1 FROM system_role WHERE (id=991220 OR (tenant_id=1 AND code='intern_user')) AND NOT (id=991220 AND tenant_id=1 AND BINARY code=BINARY 'intern_user' AND deleted=0)))
 AND ((SELECT COUNT(*) FROM system_menu WHERE id IN (2,1243,1090,1091,6800,6807) AND status=0 AND deleted=0)=6)
 AND (NOT EXISTS(SELECT 1 FROM system_menu WHERE id BETWEEN 991210 AND 991215 AND NOT (deleted=0 AND type=3 AND permission=CASE id WHEN 991210 THEN 'intern-user:time-maintenance:file-upload-time:update' WHEN 991211 THEN 'intern-user:time-maintenance:file-upload-time-audit:query' WHEN 991212 THEN 'intern-user:time-maintenance:dcc-published-time:update' WHEN 991213 THEN 'intern-user:time-maintenance:dcc-published-time-audit:query' WHEN 991214 THEN 'intern-user:time-maintenance:dcc-obsoleted-time:update' WHEN 991215 THEN 'intern-user:time-maintenance:dcc-obsoleted-time-audit:query' END)))
 AND (NOT EXISTS(SELECT 1 FROM system_menu WHERE permission LIKE 'intern-user:time-maintenance:%' AND id NOT BETWEEN 991210 AND 991215))
 THEN 'TARGET_PREFLIGHT_PASS:20260922_intern_user_time_maintenance_module' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260922_intern_user_time_maintenance_module:TARGET_PREREQUISITE_OR_CONFLICT' END;

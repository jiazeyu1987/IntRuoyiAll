-- release-target-preflight: migrationId=20261102_system_permission_audit_query_menu; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900183 AND parent_id=101
      AND component='system/role/index' AND status=0 AND deleted=b'0')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_system_permission_audit_query_menu:CONDITION_1'
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE permission='system:gxp-audit:query')>1
      OR EXISTS (SELECT 1 FROM system_menu WHERE permission='system:gxp-audit:query'
        AND (parent_id<>900183 OR type<>3 OR status<>0 OR deleted<>b'0'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_system_permission_audit_query_menu:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261102_system_permission_audit_query_menu' END;

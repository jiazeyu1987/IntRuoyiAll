-- release-target-preflight: migrationId=20260813_dcc_explicit_data_scope_permissions; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 (SELECT COUNT(*) FROM system_menu WHERE path='project-code' AND deleted=b'0')=1
 AND EXISTS(SELECT 1 FROM system_menu WHERE permission='dcc:controlled-file:query' AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission IN('dcc:project-code:scope:all','dcc:controlled-file:scope:all') AND deleted=b'0' GROUP BY permission HAVING COUNT(*)>1)
 AND NOT EXISTS(SELECT 1 FROM system_menu s WHERE s.deleted=b'0' AND ((s.permission='dcc:project-code:scope:all' AND s.parent_id<>(SELECT id FROM system_menu WHERE path='project-code' AND deleted=b'0')) OR (s.permission='dcc:controlled-file:scope:all' AND s.parent_id<>(SELECT id FROM system_menu WHERE permission='dcc:controlled-file:query' AND deleted=b'0' ORDER BY id LIMIT 1))))
 THEN 'TARGET_PREFLIGHT_PASS:20260813_dcc_explicit_data_scope_permissions' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260813_dcc_explicit_data_scope_permissions:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;

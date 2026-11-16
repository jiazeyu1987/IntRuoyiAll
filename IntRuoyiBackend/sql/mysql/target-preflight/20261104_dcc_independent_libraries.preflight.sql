-- release-target-preflight: migrationId=20261104_dcc_independent_libraries; allowedEnvironments=test,backup,prod
-- Check existing menu/package baseline; library_scope is added and validated by this migration before its DML.
SELECT CASE
 WHEN (EXISTS(SELECT 1 FROM system_menu WHERE id=605072140 AND permission<>'dcc:controlled-file:archive:query')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261104_dcc_independent_libraries:CONDITION_1'
 WHEN (NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6807 AND permission='dcc:controlled-file:query' AND deleted=0)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261104_dcc_independent_libraries:CONDITION_2'
 WHEN (EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND NOT JSON_VALID(menu_ids))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261104_dcc_independent_libraries:CONDITION_3'
 ELSE 'TARGET_PREFLIGHT_PASS:20261104_dcc_independent_libraries' END;

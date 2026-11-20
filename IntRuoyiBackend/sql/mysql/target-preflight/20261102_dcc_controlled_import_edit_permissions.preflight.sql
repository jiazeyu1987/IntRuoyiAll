-- release-target-preflight: migrationId=20261102_dcc_controlled_import_edit_permissions; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND code='doc_control' AND status=0 AND deleted=b'0')<>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_dcc_controlled_import_edit_permissions:CONDITION_1'
 WHEN (NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6800 AND deleted=b'0')
    OR NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6807 AND parent_id=6800 AND permission='dcc:controlled-file:query' AND deleted=b'0')
    OR (SELECT MAX(id) FROM system_role WHERE tenant_id=1 AND code='doc_control' AND status=0 AND deleted=b'0') IS NULL) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_dcc_controlled_import_edit_permissions:CONDITION_2'
 WHEN (EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND NOT JSON_VALID(menu_ids))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_dcc_controlled_import_edit_permissions:CONDITION_3'
 WHEN (EXISTS(SELECT 1 FROM system_menu WHERE (id=605072130 OR permission='dcc:controlled-file:import')
   AND NOT(id=605072130 AND permission='dcc:controlled-file:import' AND parent_id=6800 AND component='dcc/controlled-file/controlled-upload/index' AND deleted=b'0'))
 OR EXISTS(SELECT 1 FROM system_menu WHERE (id=605072131 OR permission='dcc:controlled-file:edit')
   AND NOT(id=605072131 AND permission='dcc:controlled-file:edit' AND parent_id=6807 AND type=3 AND deleted=b'0'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_dcc_controlled_import_edit_permissions:CONDITION_4'
 ELSE 'TARGET_PREFLIGHT_PASS:20261102_dcc_controlled_import_edit_permissions' END;
